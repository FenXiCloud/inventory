package com.flyemu.share.entity.inventory;

import com.flyemu.share.enums.OperationType;
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

/**
 * 库存成本批次：所有入库均建层；出库统一扣数量，成本按账套成本法估值。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class InventoryCostBatch implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("批次号")
    @Column(length = 64, nullable = false)
    private String batchNo;

    @Comment("产品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("仓库ID")
    @Column(nullable = false)
    private Long warehouseId;

    @Comment("入库日期")
    @Column(nullable = false)
    private LocalDate inboundDate;

    @Comment("来源单据ID")
    @Column(nullable = false)
    private Long inboundOrderId;

    @Comment("来源单据类型")
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private OperationType inboundOrderType;

    @Comment("来源明细ID")
    private Long inboundItemId;

    @Comment("入库数量")
    @Column(nullable = false)
    private Integer qtyIn;

    @Comment("剩余数量")
    @Column(nullable = false)
    private Integer qtyRemain;

    @Comment("入库单位成本")
    @Column(nullable = false, precision = 18, scale = 6)
    private BigDecimal unitCost;

    @Comment("剩余成本金额")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalCostRemain;

    @Comment("供应商ID")
    private Long supplierId;

    @Comment("是否关闭")
    @Column(nullable = false)
    private Boolean closed;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
