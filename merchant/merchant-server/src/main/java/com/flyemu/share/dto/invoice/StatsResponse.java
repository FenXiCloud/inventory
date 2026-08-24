package com.flyemu.share.dto.invoice;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StatsResponse {
    private BigDecimal totalQuota;       // 总开票额度
    private BigDecimal usedQuota;        // 已使用额度
    private BigDecimal remainingQuota;   // 剩余额度
    private Integer invoiceCount;        // 开票份数
}
