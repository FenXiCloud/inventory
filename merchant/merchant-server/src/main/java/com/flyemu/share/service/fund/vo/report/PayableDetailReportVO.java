package com.flyemu.share.service.fund.vo.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 应付账款明细报表 */
@JsonInclude()
@Data
public class PayableDetailReportVO {
    // 供应商
    private String supplierName;
    // 业务员
    private String staffName;
    // 单据日期
    private LocalDate orderDate;
    // 单据编号
    private String orderNo;
    // 业务类型
    private String businessType;
    // 增加应付款
    private BigDecimal payableAmount;
    // 增加预付款
    private BigDecimal prepaymentAmount;
    // 应付款余额
    private BigDecimal balance;
    // 备注
    private String remarks;
}
