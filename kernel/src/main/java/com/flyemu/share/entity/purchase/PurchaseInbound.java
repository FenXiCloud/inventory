package com.flyemu.share.entity.purchase;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.LazyCollection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class PurchaseInbound implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("采购订单ID")
    private Long orderId;

    @Comment("来源销售订单ID")
    private Long sourceSalesOrderId;

    @Comment("来源类型：以销定购/普通采购")
    @Column(length = 20)
    private String sourceType = "普通采购";

    @Comment("供货商ID")
    private Long supplierId;

    @Comment("入库日期")
    private LocalDate inboundDate;

    @Comment("预计到货日期")
    private LocalDate expectedDeliveryDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("已核销金额")
    private BigDecimal verifiedAmount;

    @Comment("付款金额")
    private BigDecimal paymentAmount;

    @Comment("账户ID")
    private Long accountId;

    @Comment("备注")
    private String remarks;

    @Comment("订单状态")
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Comment("基本数量和")
    @Column(precision = 18, scale = 4)
    private BigDecimal secondarySum;

    @Comment("退货数量和")
    @Column(precision = 18, scale = 4)
    private BigDecimal returnSum;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
