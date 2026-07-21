package com.flyemu.share.dto;

import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 产品价格资料行：默认采购价 + 各客户等级价格
 */
@Data
public class ProductPriceDTO {

    private Long id;

    @Comment("编码")
    private String code;

    @Comment("名称")
    private String name;

    @Comment("规格")
    private String specification;

    @Comment("默认采购价（基础单位）")
    private BigDecimal purchasePrice;

    @Comment("基础单位")
    private Long unitId;

    private String unitName;

    @Comment("产品分类")
    private Long productCategoryId;

    private String productCategoryName;

    private Long accountBookId;

    private Long merchantId;

    /**
     * 客户等级价格：customerLevelId -> price
     */
    private Map<Long, BigDecimal> levelPrices = new HashMap<>();
}
