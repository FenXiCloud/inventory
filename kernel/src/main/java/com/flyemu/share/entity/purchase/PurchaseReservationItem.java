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
@Deprecated
public class PurchaseReservationItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("进货预订主表ID")
    private Long purchaseReservationId;

    @Comment("产品ID")
    private Long productId;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("数量（以基本单位计）")
    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Comment("已转采购数量")
    @Column(precision = 18, scale = 4)
    private BigDecimal quantityOrdered;

    @Comment("辅助单位ID")
    private Long secondaryUnitId;

    @Comment("辅助单位数量")
    @Column(precision = 18, scale = 4)
    private BigDecimal secondaryQuantity;

    @Comment("换算率")
    private BigDecimal conversionRate;

    @Comment("单价（以基本单位计）")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal unitPrice;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountValue;

    @Comment("小计")
    private BigDecimal subtotal;

    @Comment("税率")
    private BigDecimal taxRate;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("关联销售预订ID")
    private Long salesReservationId;

    @Comment("关联销售预订明细ID")
    private Long salesReservationItemId;

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

    @Comment("备注")
    private String remark;
}
