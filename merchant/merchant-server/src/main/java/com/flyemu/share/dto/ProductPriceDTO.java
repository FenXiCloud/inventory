package com.flyemu.share.dto;

import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ProductPriceDTO {

    private Integer id;

    private String code;

    private String name;

    private String title;

    private BigDecimal purchasePrice;

    private Integer productCategoryId;

    private String productCategoryName;

    private String specification;

    private String imgPath;

    private Integer unitId;

    private String unitName;

    private Boolean enableMultiUnit;

    private Boolean enabled;

    private Date createDate;

    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    private Integer sort;

    private String remarks;
    
    private String pinyin;

    private Integer stockQuantity;

    private Integer alertQuantity;

    @Comment("最高采购价")
    private BigDecimal maxPurchasePrice ;
    @Comment("最近采购价")
    private BigDecimal recentlyPurchasePrice ;

    @Comment("零售客户价")
    private BigDecimal retailCustomerPrice;
    @Comment("批发客户价")
    private BigDecimal wholesaleCustomerPrice;
    @Comment("VIP客户价")
    private BigDecimal vipCustomerPrice;

    @Comment("最低销售价")
    private BigDecimal minSalesPrice;
    @Comment("最近销售价")
    private BigDecimal recentlySalesPrice;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
