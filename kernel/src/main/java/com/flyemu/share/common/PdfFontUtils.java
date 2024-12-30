package com.flyemu.share.common;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class PdfFontUtils {

    /**
     * 字体
     */
    private static BaseFont baseFont = null;

    static {
        try {
            baseFont = BaseFont.createFont("simsun.ttc,1", BaseFont.IDENTITY_H, BaseFont.NOT_EMBEDDED);
        } catch (DocumentException e) {
            log.error("字体初始化失败！", e);
        } catch (IOException e) {
            log.error("simsun.ttc字体不存在！", e);
        }
    }

    public static Paragraph getFont(int type, String text) {
        return getFont(type, text, type);
    }

    public static Paragraph getFont(String text, int fontSize) {
        return getFont(fontSize, text, 9, false);
    }

    /**
     * 文档超级  排版
     *
     * @param type 1-标题 2-标题一  3-标题二 4-标题三  5-正文  6-左对齐
     */
    public static Paragraph getFont(int type, String text, int align) {
        Font font = new Font(baseFont);
        if (1 == type) {//1-标题
            font.setSize(16f);
            font.setStyle(Font.BOLD);
        } else if (2 == type) {//2-标题一
            font.setSize(16f);
            font.setStyle(Font.BOLD);
        } else if (3 == type) {//3-标题二
            font.setSize(14f);
            font.setStyle(Font.BOLD);
        } else if (4 == type) {//4-标题三
            font.setSize(12.5f);
        } else if (5 == type) {//5-正文
            font.setSize(10.5f);
        } else if (6 == type) {//6-左对齐
            font.setSize(8.5f);
        } else {
            font.setSize(8.5f);//默认大小
        }
        //注： 字体必须和 文字一起new
        Paragraph paragraph = new Paragraph(text, font);
        if (1 == align) {
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);//居中
            paragraph.setSpacingBefore(10f);//上间距
            paragraph.setSpacingAfter(10f);//下间距
        } else if (2 == align) {//2-标题一
            paragraph.setAlignment(Element.ALIGN_JUSTIFIED); //默认
            paragraph.setSpacingBefore(2f);//上间距
            paragraph.setSpacingAfter(2f);//下间距
        } else if (3 == align) {
            paragraph.setSpacingBefore(2f);//上间距
            paragraph.setSpacingAfter(1f);//下间距
        } else if (4 == align) {//4-标题三
            //paragraph.setAlignment(Element.ALIGN_RIGHT);//右对齐
            paragraph.setSpacingBefore(2f);//上间距
            paragraph.setSpacingAfter(2f);//下间距
        } else if (5 == align) {
            paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
            paragraph.setFirstLineIndent(24);//首行缩进
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (6 == align) {//左对齐
            paragraph.setAlignment(Element.ALIGN_LEFT);
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (7 == align) {//左对齐
            paragraph.setAlignment(Element.ALIGN_RIGHT);
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (8 == align) {//左对齐
            paragraph.setAlignment(Element.ALIGN_CENTER);
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (9 == align) {
            paragraph.setAlignment(Element.ALIGN_CENTER);
//            paragraph.setSpacingBefore(1f);//上间距
            paragraph.setSpacingAfter(1f);//下间距
        }
        return paragraph;
    }

    public static Paragraph getFont(int fontSize, String text, int align, Boolean isBold) {
        Font font = new Font(baseFont);
        font.setSize(fontSize);
        if (isBold) {
            font.setStyle(Font.BOLD);
        }
        //注： 字体必须和 文字一起new
        Paragraph paragraph = new Paragraph(text, font);
        if (1 == align) {
            paragraph.setAlignment(Paragraph.ALIGN_CENTER);//居中
            paragraph.setSpacingBefore(10f);//上间距
            paragraph.setSpacingAfter(10f);//下间距
        } else if (2 == align) {//2-标题一
            paragraph.setAlignment(Element.ALIGN_JUSTIFIED); //默认
            paragraph.setSpacingBefore(2f);//上间距
            paragraph.setSpacingAfter(2f);//下间距
        } else if (3 == align) {
            paragraph.setSpacingBefore(2f);//上间距
            paragraph.setSpacingAfter(1f);//下间距
        } else if (4 == align) {//4-标题三
            //paragraph.setAlignment(Element.ALIGN_RIGHT);//右对齐
            paragraph.setSpacingBefore(2f);//上间距
            paragraph.setSpacingAfter(2f);//下间距
        } else if (5 == align) {
            paragraph.setAlignment(Element.ALIGN_JUSTIFIED);
            paragraph.setFirstLineIndent(24);//首行缩进
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (6 == align) {//左对齐
            paragraph.setAlignment(Element.ALIGN_LEFT);
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (7 == align) {//左对齐
            paragraph.setAlignment(Element.ALIGN_RIGHT);
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (8 == align) {//左对齐
            paragraph.setAlignment(Element.ALIGN_CENTER);
            paragraph.setSpacingBefore(3f);//上间距
            paragraph.setSpacingAfter(3f);//下间距
        } else if (9 == align) {
            paragraph.setAlignment(Element.ALIGN_CENTER);
            paragraph.setSpacingBefore(1f);//上间距
            paragraph.setSpacingAfter(1f);//下间距
        }
        return paragraph;
    }

    /**
     * 添加 cell
     *
     * @param table
     * @param text
     * @param colspan
     * @param border
     */
    public static PdfPCell addCell(PdfPTable table, int align, String text, int colspan, int border) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfFontUtils.getFont(4, text)));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setMinimumHeight(21);
        cell.setColspan(colspan);
        cell.setBorder(border);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        table.addCell(cell);
        return cell;
    }

    /**
     * 添加 cell(带字体大小)
     *
     * @param table
     * @param text
     * @param colspan
     * @param border
     */
    public static PdfPCell addCellFont(PdfPTable table, int align, String text, int colspan, int border, int fontSize) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfFontUtils.getFont(text, fontSize)));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setMinimumHeight(21);
        cell.setColspan(colspan);
        cell.setBorder(border);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(3);
        table.addCell(cell);
        return cell;
    }

    /**
     * 添加 cell
     *
     * @param table
     * @param text
     * @param rowspan
     * @param border
     */
    public static PdfPCell addRowsCell(PdfPTable table, int align, String text, int rowspan, int border) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfFontUtils.getFont(5, text)));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setMinimumHeight(20);
        cell.setRowspan(rowspan);
//        cell.setBorder(border);
        //cell.setPaddingTop(10);
        //cell.setPaddingBottom(10);
        table.addCell(cell);
        return cell;
    }

    public static PdfPCell newCell(int colspan, int border) {
        PdfPCell cell = new PdfPCell();
        cell.setMinimumHeight(20);
        cell.setColspan(colspan);
        cell.setBorder(border);
        return cell;
    }

    public static PdfPCell addCell(PdfPTable table, int align, String text, int colspan, int rowspan, int border) {
        PdfPCell cell = new PdfPCell(new Phrase(PdfFontUtils.getFont(6, text)));
        cell.setHorizontalAlignment(align);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setMinimumHeight(20);
        cell.setColspan(colspan);
        cell.setRowspan(rowspan);
        cell.setBorder(border);
        table.addCell(cell);
        return cell;
    }

    public static PdfPCell addCell(PdfPTable table, String text, int colspan, int border) {
        return addCell(table, Element.ALIGN_LEFT, text, colspan, border);
    }

    public static PdfPCell addCell(PdfPTable table, String text) {
        return addCell(table, Element.ALIGN_LEFT, text, 1, PdfPCell.BOX);
    }

    public static PdfPCell addCell(PdfPTable table, String text, int colspan) {
        return addCell(table, Element.ALIGN_LEFT, text, colspan, PdfPCell.BOX);
    }

    public static PdfPCell addCell(PdfPTable table, int align, String text) {
        return addCell(table, align, text, 1, PdfPCell.BOX);
    }
}
