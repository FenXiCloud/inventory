package com.flyemu.share.service.fund.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 供应商对账单汇总 VO
 */
@Data
public class SupplierStatementSummaryVO {
    /**
     * 期初余额
     */
    private BigDecimal openingBalance;

    /**
     * 本期采购金额合计
     */
    private BigDecimal totalPurchaseAmount;

    /**
     * 本期应付金额合计
     */
    private BigDecimal totalCopeWithAmount;

    /**
     * 本期实付金额合计
     */
    private BigDecimal totalActualPaymentAmount;

    /**
     * 本期优惠金额合计
     */
    private BigDecimal totalPreferentialAmount;

    /**
     * 期末余额
     */
    private BigDecimal closingBalance;
}
