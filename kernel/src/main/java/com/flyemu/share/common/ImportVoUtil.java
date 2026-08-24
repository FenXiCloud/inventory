package com.flyemu.share.common;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.poi.excel.ExcelReader;
import cn.hutool.poi.excel.ExcelWriter;
import com.flyemu.share.annotation.ColName;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Excel 导入导出列别名工具 */
@Slf4j
public final class ImportVoUtil {

    /**
     * 获取列别名
     *
     * @param type
     * @return
     */
    public static void setHeaderAlias(Class<?> type, ExcelWriter writer) {
        setHeaderAlias(type, writer, false);
    }

    /**
     * @param type
     * @param revert
     * @return
     */
    public static void setHeaderAlias(Class<?> type, ExcelWriter writer, boolean revert) {
        Map<String, String> headerAlias = new HashMap<>();
        Field[] colFields = ReflectUtil.getFields(type, field -> field.isAnnotationPresent(ColName.class));
        List<String> as = new ArrayList<>();
        for (Field colField : colFields) {
            if (revert) {
                writer.addHeaderAlias(colField.getAnnotation(ColName.class).value(), colField.getName());
            } else {
                as.add("writer.addHeaderAlias(\"" + colField.getName() + "\", \"" + colField.getAnnotation(ColName.class).value() + "\");");
                writer.addHeaderAlias(colField.getName(), colField.getAnnotation(ColName.class).value());
            }
        }
        log.info("\n{}", CollUtil.join(as, "\n"));
    }

    public static void setHeaderAlias(Class<?> type, ExcelReader reader) {
        Field[] colFields = ReflectUtil.getFields(type, field -> field.isAnnotationPresent(ColName.class));
        for (Field colField : colFields) {
            reader.addHeaderAlias(colField.getAnnotation(ColName.class).value(), colField.getName());
        }
    }

    /**
     * 用POI DataFormatter读取Excel，避免POI 4.1 + JDK 21的日期单元格bug
     */
    public static <T> List<T> readImportFile(MultipartFile file, Class<T> type) throws IOException {
        byte[] bytes = file.getBytes();
        String fileName = file.getOriginalFilename();
        List<List<String>> rawRows;

        if (fileName != null && fileName.toLowerCase().endsWith(".xlsx")) {
            // 纯XML解析，绕过POI/xmlbeans
            try {
                rawRows = XlsxReader.read(bytes);
            } catch (Exception e) {
                throw new IOException("读取xlsx失败: " + e.getMessage(), e);
            }
        } else {
            // .xls 用HSSF，无xmlbeans依赖
            try {
                org.apache.poi.hssf.usermodel.HSSFWorkbook wb = new org.apache.poi.hssf.usermodel.HSSFWorkbook(new java.io.ByteArrayInputStream(bytes));
                rawRows = readSheet(wb.getSheetAt(0));
                wb.close();
            } catch (Exception e) {
                throw new IOException("读取xls失败: " + e.getMessage(), e);
            }
        }
        return parseImportRows(rawRows, type);
    }

    private static List<List<String>> readSheet(Sheet sheet) {
        DataFormatter formatter = new DataFormatter();
        List<List<String>> rows = new ArrayList<>();
        for (Row row : sheet) {
            List<String> rowData = new ArrayList<>();
            for (int i = 0; i < row.getLastCellNum(); i++) {
                Cell cell = row.getCell(i);
                rowData.add(cell == null ? "" : formatter.formatCellValue(cell).trim());
            }
            rows.add(rowData);
        }
        return rows;
    }

    /**
     * 从原始行数据解析ImportVo，避免POI 4.1日期单元格兼容问题
     */
    public static <T> List<T> parseImportRows(List<List<String>> rawRows, Class<T> type) {
        if (CollUtil.isEmpty(rawRows) || rawRows.size() < 2) return Collections.emptyList();

        // 第一行是header
        List<String> headerRow = rawRows.get(0);
        Map<String, Integer> headerMap = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i++) {
            String h = headerRow.get(i).trim();
            if (StrUtil.isNotBlank(h)) headerMap.put(h, i);
        }

        // 构建 @Alias header->field 映射
        Map<String, Field> aliasFieldMap = new HashMap<>();
        Field[] fields = ReflectUtil.getFields(type);
        for (Field f : fields) {
            cn.hutool.core.annotation.Alias alias = f.getAnnotation(cn.hutool.core.annotation.Alias.class);
            if (alias != null) aliasFieldMap.put(alias.value(), f);
        }

        List<T> result = new ArrayList<>();
        for (int i = 1; i < rawRows.size(); i++) {
            List<String> row = rawRows.get(i);
            if (row == null || row.stream().allMatch(o -> o == null || StrUtil.isBlank(o))) continue;
            try {
                T vo = type.getDeclaredConstructor().newInstance();
                for (Map.Entry<String, Integer> e : headerMap.entrySet()) {
                    Field f = aliasFieldMap.get(e.getKey());
                    if (f == null) continue;
                    String strVal = e.getValue() < row.size() ? row.get(e.getValue()) : null;
                    if (strVal == null || StrUtil.isBlank(strVal)) continue;
                    f.setAccessible(true);
                    if (f.getType() == String.class) {
                        // Excel序列号日期转换 (5位数 = 序列号 30000~80000)
                        if (f.getName().toLowerCase().contains("date") && strVal.matches("\\d{5}")) {
                            try {
                                long serial = Long.parseLong(strVal);
                                java.time.LocalDate dt = java.time.LocalDate.of(1899, 12, 30).plusDays(serial);
                                strVal = dt.toString();
                            } catch (Exception ignored) {}
                        }
                        f.set(vo, strVal);
                    } else if (f.getType() == BigDecimal.class) {
                        f.set(vo, new BigDecimal(strVal));
                    } else if (f.getType() == Integer.class || f.getType() == int.class) {
                        f.set(vo, Integer.parseInt(strVal));
                    } else if (f.getType() == Long.class || f.getType() == long.class) {
                        f.set(vo, Long.parseLong(strVal));
                    } else if (f.getType() == Double.class || f.getType() == double.class) {
                        f.set(vo, Double.parseDouble(strVal));
                    }
                }
                result.add(vo);
            } catch (Exception ex) {
                log.warn("解析导入行 {} 失败: {}", i + 1, ex.getMessage());
            }
        }
        return result;
    }
}
