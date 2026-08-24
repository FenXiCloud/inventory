package com.flyemu.share.common;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/**
 * 纯 ZIP+XML 方式读取 .xlsx 文件，不依赖 POI/xmlbeans。
 */
public class XlsxReader {

    private static final String NS = "*";

    public static List<List<String>> read(byte[] fileBytes) throws Exception {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(fileBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), readEntry(zis));
            }
        }

        // 先读共享字符串
        List<String> sharedStrings = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            if (e.getKey().equalsIgnoreCase("xl/sharedStrings.xml")) {
                sharedStrings = parseSharedStrings(e.getValue());
                break;
            }
        }

        // 再读第一个 sheet
        for (Map.Entry<String, byte[]> e : entries.entrySet()) {
            String name = e.getKey();
            if (name.startsWith("xl/worksheets/sheet") && name.endsWith(".xml")) {
                return parseSheet(e.getValue(), sharedStrings);
            }
        }
        return Collections.emptyList();
    }

    private static byte[] readEntry(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int len;
        while ((len = zis.read(buf)) > 0) bos.write(buf, 0, len);
        return bos.toByteArray();
    }

    private static List<String> parseSharedStrings(byte[] data) throws Exception {
        List<String> list = new ArrayList<>();
        Document doc = parseXml(data);
        NodeList siNodes = doc.getElementsByTagNameNS(NS, "si");
        for (int i = 0; i < siNodes.getLength(); i++) {
            Element si = (Element) siNodes.item(i);
            NodeList tList = si.getElementsByTagNameNS(NS, "t");
            if (tList.getLength() > 0) {
                list.add(tList.item(0).getTextContent());
            } else {
                list.add("");
            }
        }
        return list;
    }

    private static List<List<String>> parseSheet(byte[] data, List<String> sharedStrings) throws Exception {
        List<List<String>> rows = new ArrayList<>();
        Document doc = parseXml(data);
        NodeList rowNodes = doc.getElementsByTagNameNS(NS, "row");

        // First pass: find max columns
        int maxCol = 0;
        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowEl = (Element) rowNodes.item(i);
            NodeList cells = rowEl.getElementsByTagNameNS(NS, "c");
            for (int j = 0; j < cells.getLength(); j++) {
                Element cell = (Element) cells.item(j);
                String ref = cell.getAttribute("r");
                if (ref != null && !ref.isEmpty()) {
                    int col = colToIndex(ref.replaceAll("\\d", ""));
                    if (col >= maxCol) maxCol = col + 1;
                }
            }
        }
        if (maxCol == 0) return rows;

        for (int i = 0; i < rowNodes.getLength(); i++) {
            Element rowEl = (Element) rowNodes.item(i);
            NodeList cells = rowEl.getElementsByTagNameNS(NS, "c");
            String[] row = new String[maxCol];
            Arrays.fill(row, "");

            for (int j = 0; j < cells.getLength(); j++) {
                Element cell = (Element) cells.item(j);
                String ref = cell.getAttribute("r");
                if (ref == null || ref.isEmpty()) continue;
                int col = colToIndex(ref.replaceAll("\\d", ""));
                String type = cell.getAttribute("t");
                String value = "";
                NodeList vList = cell.getElementsByTagNameNS(NS, "v");
                if (vList.getLength() > 0) value = vList.item(0).getTextContent();

                String display;
                if ("s".equals(type) && !value.isEmpty()) {
                    int idx = Integer.parseInt(value);
                    display = idx < sharedStrings.size() ? sharedStrings.get(idx) : value;
                } else if ("b".equals(type)) {
                    display = "1".equals(value) ? "TRUE" : "FALSE";
                } else if ("inlineStr".equals(type)) {
                    NodeList isList = cell.getElementsByTagNameNS(NS, "is");
                    if (isList.getLength() > 0) {
                        NodeList tList = ((Element) isList.item(0)).getElementsByTagNameNS(NS, "t");
                        display = tList.getLength() > 0 ? tList.item(0).getTextContent() : "";
                    } else {
                        display = value;
                    }
                } else {
                    display = value;
                }
                row[col] = display != null ? display : "";
            }
            // Skip completely empty rows
            boolean allEmpty = true;
            for (String s : row) { if (!s.isEmpty()) { allEmpty = false; break; } }
            if (!allEmpty) rows.add(Arrays.asList(row));
        }
        return rows;
    }

    private static int colToIndex(String col) {
        int idx = 0;
        for (char c : col.toUpperCase().toCharArray()) {
            idx = idx * 26 + (c - 'A' + 1);
        }
        return idx - 1;
    }

    private static Document parseXml(byte[] data) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new InputSource(new ByteArrayInputStream(data)));
    }
}
