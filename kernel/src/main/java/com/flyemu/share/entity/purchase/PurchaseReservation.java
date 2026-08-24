package com.flyemu.share.entity.purchase;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进货预订 - 已废弃
 * 被以销定购工作台（销售订单 → 一键生成采购入库单）替代，仅保留历史数据
 * 观察期结束后删除
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@Deprecated
public class PurchaseReservation implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("供货商ID")
    private Long supplierId;

    @Comment("下单日期")
    private LocalDate orderDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("备注")
    private String remarks;

    @Comment("订单状态")
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("转采购状态 0初始 1部分转采购 2全部转采购")
    private Integer status;

    @Comment("转采购订单ID")
    private Long purchaseOrderId;
}
