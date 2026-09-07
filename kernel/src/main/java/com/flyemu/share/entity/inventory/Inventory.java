package com.flyemu.share.entity.inventory;

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
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"productId", "warehouseId", "accountBookId", "merchantId"})
})
@DynamicUpdate
public class Inventory implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("产品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("仓库ID")
    @Column(nullable = false)
    private Long warehouseId;

    @Comment("货位ID")
    private Long locationId;

    @Comment("基础单位ID")
    private Long baseUnitId;

    @Comment("库存数量")
    private Integer currentQuantity;

    @Comment("安全库存")
    private Integer safetyStock;

    @Comment("平均成本")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal averageCost;

    @Comment("成本总计")
    private BigDecimal totalCost;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
