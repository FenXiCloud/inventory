package com.flyemu.share.dto.invoice;

import lombok.Data;

@Data
public class RedInvoiceResponse {
    private Long id;                 // 本地发票ID（用于下载PDF）
    private String redInfoNo;        // 红字信息表编号
    private String status;           // 状态
    private String invoiceCode;      // 红字发票代码
    private String invoiceNumber;    // 红字发票号码
    private String pdfId;            // PDF 标识
}
