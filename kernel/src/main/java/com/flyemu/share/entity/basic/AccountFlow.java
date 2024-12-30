package com.flyemu.share.entity.basic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @功能描述: 账户流水
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class AccountFlow implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("账户Id")
    @Column(nullable = false)
    private Long accountId;

    @Comment("单据Id")
    private Long voucherId;

    @Comment("操作类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountFlowType accountFlowType;

    @Comment("交易时间")
    private LocalDateTime flowTime;

    @Comment("金额")
    @Column(nullable = false)
    private BigDecimal amount;

    @Comment("操作人Id")
    private Long userId;

    @Comment("交易前余额")
    private BigDecimal balanceBefore;

    @Comment("交易后余额")
    private BigDecimal balanceAfter;

    @Comment("审核人")
    private Long auditId;

    @Comment("审核时间")
    private LocalDateTime auditTime;

    @Comment("备注")
    private String remarks;

    public enum AccountFlowType {
        支出, 收入, 转账
    }

}

