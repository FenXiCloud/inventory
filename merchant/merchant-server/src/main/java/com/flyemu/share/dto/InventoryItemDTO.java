package com.flyemu.share.dto;

import com.flyemu.share.enums.OperationType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryItemDTO {
    private Long id;

    @Comment("关联单据ID")
    private Long orderId;

    @Comment("产品ID")
    private Long productId;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("操作类型：期初、入库、出库、调拨")
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20)")
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Comment("变更库存数")
    private Integer quantity;

    @Comment("库存数量")
    private Integer currentQuantity;

    @Comment("平均成本")
    private BigDecimal averageCost;

    @Comment("成本总计")
    private BigDecimal totalCost;

    @Comment("单价（以基本单位计）")
    private BigDecimal unitPrice;

    @Comment("小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)")
    private BigDecimal subtotal;

    @Comment("基础单位ID")
    private Long baseUnitId;

    @Comment("批次号")
    private String batchNumber;

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

    @Comment("供应商ID")
    private Long supplierId;

    @Comment("客户ID")
    private Long customerId;

    @Comment("排序首条")
    private Boolean firstSort;

    private String productName;
    private String productCode;
    private String specification;
    private String warehouseName;
    private String unitName;

}
