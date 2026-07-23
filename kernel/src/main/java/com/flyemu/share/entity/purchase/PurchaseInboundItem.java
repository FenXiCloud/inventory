package com.flyemu.share.entity.purchase;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class PurchaseInboundItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("采购入库主表ID")
    private Long purchaseInboundId;

    @Comment("产品ID")
    private Long productId;

    @Comment("批次号")
    private String batchNumber;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("数量（以基本单位计）")
    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Comment("入库单位ID")
    private Long secondaryUnitId;

    @Comment("入库单位数量")
    @Column(precision = 18, scale = 4)
    private BigDecimal secondaryQuantity;

    @Comment("入库单价")
    @Column(precision = 18, scale = 6)
    private BigDecimal secondaryPrice;

    @Comment("换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)")
    private BigDecimal conversionRate;

    @Comment("单价（以基本单位计）")
    private BigDecimal unitPrice;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)")
    private BigDecimal subtotal;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("可退货数量")
    @Column(precision = 18, scale = 4)
    private BigDecimal returnQuantity;

}
