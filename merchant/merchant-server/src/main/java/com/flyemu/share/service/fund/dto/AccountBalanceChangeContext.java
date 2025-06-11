package com.flyemu.share.service.fund.dto;

import com.flyemu.share.entity.fund.AccountFlow;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 *@author shuaiqi
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalanceChangeContext {
    // 账户ID
    private Long accountId;
    private Long supplierId;
    private Long customerId;
    // 商户ID
    private Long merchantId;
    // 帐套ID
    private Long accountBookId;
    // 变动金额（正为收入，负为支出）
    private BigDecimal amount;
    // 单据ID
    private Long voucherId;
    // 单据编号
    private String businessNo;
    // 流水类型
    private AccountFlow.AccountFlowType flowType;
    // 操作人ID
    private Long operatorId;
    private String operatorName;
    // 备注
    private String remarks;
    private Long correspondentsId;
    private String correspondentsName;
}
