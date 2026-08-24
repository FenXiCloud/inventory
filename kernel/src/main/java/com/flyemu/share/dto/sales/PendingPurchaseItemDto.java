package com.flyemu.share.dto.sales;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 以销定购看板 - 待采购商品DTO
 */
@Data
public class PendingPurchaseItemDto {

    private Long salesOrderId;
    private String salesOrderNo;
    private String customerName;
    private LocalDate orderDate;

    private Long productId;
    private String productCode;
    private String productName;
    private String specification;
    private String unitName;
    private Long baseUnitId;
    private Long warehouseId;
    private String warehouseName;

    /** 销售订单数量 */
    private BigDecimal orderQuantity;

    /** 已出库数量 */
    private BigDecimal outboundQuantity;

    /** 未出库数量 */
    private BigDecimal pendingOutboundQuantity;

    /** 已采购数量（已审核的采购入库单） */
    private BigDecimal purchasedQuantity;

    /** 待采购数量 */
    private BigDecimal pendingPurchaseQuantity;

    /** 建议供应商ID */
    private Long supplierId;

    /** 建议供应商名称 */
    private String supplierName;

    /** 最近采购单价 */
    private BigDecimal lastPurchasePrice;

    /** 采购状态文本 */
    private String purchaseStatusText;
}
