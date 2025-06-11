package com.flyemu.share.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

@Data
public class InventoryItemReportDto implements Serializable {

    /**
     * 明细id
     */
    private Long id;

    private String batchNumber;

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
    private String operationType;

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

    /**
     * 仓库id
     */
    private Long warehouseId;

    /**
     * 基本单位成本
     */
    private BigDecimal unitPrice;

    /**
     * 结存数量
     */
    private Integer currentQuantity;

    /**
     * 结存成本
     */
    private BigDecimal totalCost;

    /**
     * 结存数量
     */
    private Integer summaryQuantity;

    /**
     * 结存成本
     */
    private BigDecimal summaryCost;

    /**
     * 单位数量
     */
    private Integer quantity;

    /**
     * 成本
     */
    private BigDecimal subtotal;

    /**
     * 结存单位成本
     */
    private BigDecimal averageCost;

    /**
     * 结存单位成本
     */
    private BigDecimal summaryAverage;

    private Long supplierId;

    /**
     * 往来单位
     */
    private String supplierName;

    private Long customerId;

    /**
     * 往来单位
     */
    private String customerName;

    /**
     * 凭证id
     */
    private String voucherId;

    /**
     * 凭证code
     */
    private String voucherCode;

    /**
     * 单据日期
     */
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd")
    private Date inventoryDate;

}
