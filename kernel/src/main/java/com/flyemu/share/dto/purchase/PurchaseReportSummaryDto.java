package com.flyemu.share.dto.purchase;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)

public class PurchaseReportSummaryDto {

    /**
     * 商品编码
     */
    private Long productId;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 供货商名称
     */
    private String supplierName;

    /**
     * 仓库名称
     */
    private String warehouseName;

    /**
     * 订单类型
     */
    private String orderType;


    /**
     * 基本单位名称
     */
    private String baseUnitName;

    /**
     * 基本数量
     */
    private Double baseQuantitySum;

    /**
     * 采购金额
     */
    private BigDecimal subtotalSum;
}
