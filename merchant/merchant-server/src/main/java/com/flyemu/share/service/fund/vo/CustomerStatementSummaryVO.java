package com.flyemu.share.service.fund.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户对账单汇总 VO
 */
@Data
public class CustomerStatementSummaryVO {
    /**
     * 期初余额
     */
    private BigDecimal openingBalance;

    /**
     * 本期销售金额合计
     */
    private BigDecimal totalSalesAmount;

    /**
     * 本期应收金额合计
     */
    private BigDecimal totalReceivableAmount;

    /**
     * 本期实收金额合计
     */
    private BigDecimal totalPaidUpAmount;

    /**
     * 本期优惠金额合计
     */
    private BigDecimal totalPreferentialAmount;

    /**
     * 期末余额
     */
    private BigDecimal closingBalance;
}
