package com.flyemu.share.service.fund.vo.report;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 应收账款明细表
 *@author shuaiqi
 */
@JsonInclude
@Data
public class ReceivableDetailReportVO {
    // 客户
    private String customerName;
    // 单据日期
    private LocalDate orderDate;
    // 单据编号
    private String orderNo;
    // 业务类型（销售收款 / 预收款）
    private String businessType;
    // 增加应收款金额
    private BigDecimal receivableAmount;
    // 增加预收款金额
    private BigDecimal prepaymentAmount;
    // 应收款余额 = 应核销金额 - 已核销金额
    private BigDecimal balance;
    // 销售人员
    private String staffName;
    // 备注
    private String remarks;
}