package com.flyemu.share.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class InventoryReportDto implements Serializable {

    /**
     * 商品id
     */
    private Long productId;

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
     * 商品单位
     */
    private String productUnitName;

    /**
     * 单位售价（零售客户价）
     */
    private java.math.BigDecimal retailCustomerPrice;

    /**
     * 预计进货价（基础单位），利润 fallback
     */
    private java.math.BigDecimal purchasePrice;
}
