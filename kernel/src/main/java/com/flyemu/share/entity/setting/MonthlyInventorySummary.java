package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"productId", "warehouseId", "period", "merchantId", "accountBookId"})
})
@Comment("月结库存表")
public class MonthlyInventorySummary implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("商品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("仓库ID")
    @Column(nullable = false)
    private Long warehouseId;

    @Comment("结账期间（月末日期，如：2026-08-31）")
    @Column(nullable = false)
    private LocalDate period;

    @Comment("期初数量")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal beginQty = BigDecimal.ZERO;

    @Comment("期初金额")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal beginAmount = BigDecimal.ZERO;

    @Comment("本期入库数量")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal inQty = BigDecimal.ZERO;

    @Comment("本期入库金额")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal inAmount = BigDecimal.ZERO;

    @Comment("本期出库数量")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal outQty = BigDecimal.ZERO;

    @Comment("本期出库金额")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal outAmount = BigDecimal.ZERO;

    @Comment("期末数量")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal endQty = BigDecimal.ZERO;

    @Comment("期末金额")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal endAmount = BigDecimal.ZERO;

    @Comment("期末成本价")
    @Column(precision = 18, scale = 4)
    private BigDecimal endCostPrice;

    @Comment("商品编码")
    @Column(length = 64)
    private String productCode;

    @Comment("商品名称")
    @Column(length = 255)
    private String productName;

    @Comment("仓库编码")
    @Column(length = 32)
    private String warehouseCode;

    @Comment("仓库名称")
    @Column(length = 32)
    private String warehouseName;

    @Comment("单位名称")
    @Column(length = 32)
    private String unitName;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        // 计算期末成本价
        if (endQty != null && endQty.compareTo(BigDecimal.ZERO) > 0 && endAmount != null) {
            this.endCostPrice = endAmount.divide(endQty, 4, BigDecimal.ROUND_HALF_UP);
        }
    }
}
