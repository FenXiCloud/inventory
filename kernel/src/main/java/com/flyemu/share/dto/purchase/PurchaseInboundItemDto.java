package com.flyemu.share.dto.purchase;

import com.flyemu.share.dto.AuxiliaryUnitPrice;
import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
     * 来源采购订单ID（分单入库用）
     */
    private Long purchaseOrderId;

    /**
     * 来源采购订单行ID（对应PurchaseOrderItem.id）
     */
    private Long purchaseOrderItemId;

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
    private BigDecimal quantity;

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
    private BigDecimal secondaryQuantity;

    /**
     * 退货数量
     */
    private BigDecimal returnQuantity;

    /**
     * 采购单价
     */
    private BigDecimal secondaryPrice;

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

    /**
     * 商品可用单位列表（基本单位在前，rate=1；仅启用辅助单位的商品返回，用于前端单位下拉/换算）
     */
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

}
