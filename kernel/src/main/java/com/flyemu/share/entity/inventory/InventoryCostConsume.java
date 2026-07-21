package com.flyemu.share.entity.inventory;

import com.flyemu.share.enums.CostingMethod;
import com.flyemu.share.enums.OperationType;
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
 * 出库成本耗用明细：记录出库扣了哪些批次（反审与追溯用）。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class InventoryCostConsume implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("出库单据ID")
    @Column(nullable = false)
    private Long outboundOrderId;

    @Comment("出库单据类型")
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private OperationType outboundOrderType;

    @Comment("出库明细ID")
    private Long outboundItemId;

    @Comment("成本批次ID")
    @Column(nullable = false)
    private Long batchId;

    @Comment("产品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("仓库ID")
    @Column(nullable = false)
    private Long warehouseId;

    @Comment("扣减数量")
    @Column(nullable = false)
    private Integer qty;

    @Comment("记账单位成本")
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal unitCostUsed;

    @Comment("成本金额")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal costAmount;

    @Comment("当时成本法")
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private CostingMethod costingMethod;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
