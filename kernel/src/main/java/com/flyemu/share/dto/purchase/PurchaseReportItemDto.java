package com.flyemu.share.dto.purchase;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = false)

public class PurchaseReportItemDto {
    private Long id;

    /**
     * 单据编号
     */
    private String orderNo;

    /**
     * 商品编码
     */
    private String productCode;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 规格
     */
    private String spec;

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
     * 下单日期
     */
    private LocalDate orderDate;


    /**
     * 采购单位名称
     */
    private String secondaryUnitName;

    /**
     * 采购数量
     */
    private Double secondaryQuantity;

    /**
     * 采购单价
     */
    private Double secondaryPrice;

    /**
     * 采购金额
     */
    private BigDecimal subtotal;
}
