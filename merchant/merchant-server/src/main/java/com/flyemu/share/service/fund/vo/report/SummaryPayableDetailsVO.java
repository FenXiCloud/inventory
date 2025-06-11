package com.flyemu.share.service.fund.vo.report;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 应付账款汇总列表
 *@author shuaiqi
 */
@Data
public class SummaryPayableDetailsVO {
    /**
     * 供应商类别
     */
    private String supplierCategory;

    /**
     * 供应商编号
     */
    private String supplierCode;

    /**
     * 供应商名称
     */
    private String supplierName;

    /**
     * 期初余额
     */
    private BigDecimal openingBalance;

    /**
     * 本期应付
     */
    private BigDecimal currentPayable;

    /**
     * 本期付款
     */
    private BigDecimal currentPayment;

    /**
     * 期末余额
     */
    private BigDecimal closingBalance;
}
