package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@JsonInclude()
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class OrderReceiptItem implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("收款单ID")
    private Long receiptId;

    @Comment("账户ID")
    private Long accountId;

    @Comment("收入金额")
    private BigDecimal amount;

    @Comment("结算方式")
    private Long paymentMethodId;

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
    @Comment("关联销售单ID")
    private Long salesOrderId;
    @Comment("关联销售单编号")
    private String salesOrderNo;
    @Comment("业务类型")
    private Integer businessType;
    @Comment("业务日期")
    private LocalDate businessDate;

    @Comment("单据金额")
    private BigDecimal documentAmount;

    @Comment("已核销金额")
    private BigDecimal verifiedAmount;

    @Comment("未核销金额")
    private BigDecimal unverifiedAmount;

    @Comment("本次核销金额")
    private BigDecimal currentVerifyAmount;
}
