package com.flyemu.share.service.fund.vo.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class SummaryReceivableDetailsPageVO {
    private List<SummaryReceivableDetailsVO> receivableDetailsList;
    private long receivableDetailsListTotal;
    private BigDecimal totalOpeningBalance;
    private BigDecimal totalCurrentReceivable;
    private BigDecimal totalCurrentReceipt;
    private BigDecimal totalClosingBalance;
}
