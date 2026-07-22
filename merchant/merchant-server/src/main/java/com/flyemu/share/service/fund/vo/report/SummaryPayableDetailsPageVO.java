package com.flyemu.share.service.fund.vo.report;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** 应付账款汇总 */
@Data
public class SummaryPayableDetailsPageVO {
    List<SummaryPayableDetailsVO> payableDetailsList;
    long payableDetailsListTotal;
    private BigDecimal totalOpeningBalance; // 总期初余额
    private BigDecimal totalCurrentPayable; // 总本期应付
    private BigDecimal totalCurrentPayment; // 总本期付款
    private BigDecimal totalClosingBalance; // 总期末余额

}
