package com.flyemu.share.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class InventoryReportDto implements Serializable {

    /**
     * 库存id
     */
    private Long id;

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
}
