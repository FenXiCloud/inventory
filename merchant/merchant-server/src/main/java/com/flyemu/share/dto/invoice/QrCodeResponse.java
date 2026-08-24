package com.flyemu.share.dto.invoice;

import lombok.Data;

@Data
public class QrCodeResponse {
    /** 认证ID (rzid)，用于查询扫脸状态 */
    private String qrId;
    /** 二维码原始文本 (ewm) */
    private String qrImage;
}
