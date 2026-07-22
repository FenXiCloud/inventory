package com.flyemu.share.dto.purchase;

import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
public class PurchaseInboundItemDto {

    private Long id;

    /**
     * 采购入库单id
     */
    private Long purchaseInboundId;

    /**
     * 采购入库单orderNo
     */
    private String purchaseInboundOrderNo;

    /**
     * 采购明细单id
     */
    private Long purchaseInboundItemId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 基本单位ID
     */
    private Long baseUnitId;

    /**
     * 基本单位名称
     */
    private String baseUnitName;

    /**
     * 基本数量
     */
    private Double quantity;

    /**
     * 采购单位ID
     */
    private Long secondaryUnitId;

    /**
     * 采购单位名称
     */
    private String secondaryUnitName;

    /**
     * 采购数量
     */
    private Double secondaryQuantity;

    /**
     * 退货数量
     */
    private Double returnQuantity;

    /**
     * 采购单价
     */
    private Double secondaryPrice;

    /**
     * 换算率
     */
    private BigDecimal conversionRate;

    /**
     * 基本单价
     */
    private BigDecimal unitPrice;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 小计
     */
    private BigDecimal subtotal;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库Name
     */
    private String warehouseName;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 规格
     */
    private String spec;
    /**
     * 分类
     */
    private String categoryName;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    /**
     * 备注
     */
    private String remark;

}
