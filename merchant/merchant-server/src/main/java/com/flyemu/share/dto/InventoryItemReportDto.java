package com.flyemu.share.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.flyemu.share.enums.OperationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class InventoryItemReportDto implements Serializable {

    /**
     * 明细id
     */
    private Long id;

    /**
     * 商品id
     */
    private Long productId;

    /**
     * 商品类别id
     */
    private Long productCategoryId;

    /**
     * 商品编号
     */
    private String productCode;

    /**
     * 商品图片url
     */
    private String productUrl;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 商品类别
     */
    private String productCategoryName;

    /**
     * 商品规格
     */
    private String productSpecification;

    /**
     * 商品备注
     */
    private String productRemarks;

    /**
     * 操作类型
     */
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    /**
     * 商品单位
     */
    private String unitName;

    /**
     * 单据时间
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd")
    private LocalDateTime createdAt;

    /**
     * 仓库名称
     */
    private String warehouseName;

    private Long warehouseId;

    private BigDecimal unitPrice;

    private Integer currentQuantity;

    private BigDecimal totalCost;

    private Integer quantity;

    private BigDecimal subtotal;

    private BigDecimal averageCost;

}
