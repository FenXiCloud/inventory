package com.flyemu.share.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Data
public class ProductImportVo {

    @Comment("商品编码")
    @Alias("商品编码")
    private String code;

    @Comment("商品名称")
    @Alias("商品名称")
    private String name;

    @Comment("品牌")
    @Alias("品牌")
    private String brand;

    @Comment("规格")
    @Alias("规格")
    private String specification;

    @Comment("分类")
    @Alias("分类")
    private String productCategoryName;

    @Comment("单位")
    @Alias("单位")
    private String unitName;

    @Comment("进货价")
    @Alias("进货价")
    private BigDecimal purchasePrice;

    @Comment("预警库存")
    @Alias("预警库存")
    private Integer alertQuantity;

    @Comment("备注")
    @Alias("备注")
    private String remarks;

}
