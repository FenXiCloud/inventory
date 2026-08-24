package com.flyemu.share.dto.invoice;

import lombok.Data;

@Data
public class ScanDurationResponse {
    private int currentSeconds;   // 当前扫脸间隔
    private int maxSeconds;       // 税局最大可设置时长
}
