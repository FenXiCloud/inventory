package com.flyemu.share.service.fund.vo.report;

import lombok.Data;

import java.math.BigDecimal;

/**
 *@author shuaiqi
 */
@Data
public class SummaryReceivableDetailsVO {
    private String customerCode;
    private String customerName;
    private String customerCategory;
    private BigDecimal openingBalance;
    private BigDecimal currentReceivable;
    private BigDecimal currentReceipt;
    private BigDecimal closingBalance;
}

