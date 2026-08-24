package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@JsonInclude()
public class AccountFlow implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("账户Id")
    @Column(nullable = false)
    private Long accountId;
    @Comment("账户名称")
    private String accountName;
    @Comment("业务编号")
    private String businessNo;

    @Comment("单据Id")
    private Long voucherId;
    @Comment("货商Id")
    private Long supplierId;
    @Comment("客户Id")
    private Long customerId;
    @Comment("操作类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountFlowType accountFlowType;

    @Comment("金额")
    @Column(nullable = false, columnDefinition = "decimal(18,2) default 0")
    private BigDecimal amount;
    @Comment("收入")
    private BigDecimal income;
    @Comment("支出")
    private BigDecimal spending;
    @Comment("交易对方")
    private Long correspondentsId;
    @Comment("交易对方名称")
    private String correspondentsName;
    @Comment("收付款人Id")
    private Long amountOperatorId;
    @Comment("收付款人名称")
    private String amountOperatorName;

    @Comment("交易前余额")
    private BigDecimal balanceBefore;

    @Comment("交易后余额")
    private BigDecimal balanceAfter;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum AccountFlowType {
        其他支出单, 其他收入单, 付款单, 收款单, 资金转账
    }

}

