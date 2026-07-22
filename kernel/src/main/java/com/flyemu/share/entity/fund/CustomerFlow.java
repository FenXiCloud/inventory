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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@JsonInclude()
public class CustomerFlow implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("客户Id")
    @Column(nullable = false)
    private Long customerId;

    @Comment("操作类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private CustomerFlowType customerFlowType;
    @Comment("单据Id")
    private Long businessId;
    @Comment("单据编号")
    private String businessNo;
    @Comment("单据日期")
    private LocalDate businessDate;
    @Comment("销售金额")
    private BigDecimal salesAmount;
    @Comment("优惠金额")
    private BigDecimal preferentialAmount;
    @Comment("应收金额")
    private BigDecimal receivableAmount;
    @Comment("实收金额")
    private BigDecimal paidUpAmount;
    @Comment("应收款余额")
    private BigDecimal balanceReceivables;
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

    public enum CustomerFlowType {
        期初,
        销售出库单,
        反审核_销售出库单,
        销售退货单,
        反审核_销售退货单,
        收款单,
        反审核_收款单,
        其他收入单,
        反审核_其他收入单;
    }

}

