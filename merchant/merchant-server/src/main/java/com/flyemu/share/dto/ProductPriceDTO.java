package com.flyemu.share.dto;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ProductPriceDTO {

    private Long id;

    @Comment("编码")
    private String code;

    @Comment("名称")
    private String name;

    @Comment("拼音")
    private String pinyin;

    @Comment("条码")
    private String barcode;

    @Comment("规格")
    private String specification;

    @Comment("备注")
    private String remarks;

    @Comment("预计进货价（基础单位）")
    private BigDecimal purchasePrice ;

    @Comment("排序")
    private Integer sort;

    @Comment("基础单位")
    private Long unitId;

    private String unitName;

    @Comment("是否启用辅助单位")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(nullable = false)
    @ColumnDefault("b'0'")
    private Boolean enableMultiUnit;

    @Comment("辅助单位价格")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    @Comment("商品图片")
    private String imgPath;

    @Comment("产品分类")
    private Long productCategoryId;

    private String productCategoryName;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("库存数量")
    private Integer stockQuantity;

    @Comment("预警库存")
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

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
