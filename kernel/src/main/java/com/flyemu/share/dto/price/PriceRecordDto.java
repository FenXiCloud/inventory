package com.flyemu.share.dto.price;

import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class PriceRecordDto {

    private Long id;

    @Comment("单据主表ID")
    private Long orderId;

    @Comment("单据日期")
    private Date orderDate;

    @Comment("产品ID")
    private Long productId;

    @Comment("产品name")
    private String productName;

    @Comment("产品code")
    private String productCode;

    @Comment("产品分类")
    private String productCategory;

    @Comment("货商ID")
    private Long supplierId;

    @Comment("客户ID")
    private Long customerId;

    @Comment("基本单位ID")
    private Long baseUnitId;
    @Comment("基本单位名称")
    private String unitName;
    @Comment("规格")
    private String specification;

    @Comment("数量（以基本单位计）")
    private BigDecimal quantity;

    @Comment("单价（以基本单位计）")
    private BigDecimal unitPrice;

    @Comment("辅助单位价格")
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    @Comment("价格类别")
    private PriceType priceType;

    @Comment("价格来源")
    private PriceSource priceSource;

    private Long accountBookId;

    private Long merchantId;

    /**
     * 供货商名称
     */
    private String supplierName;

    /**
     * 预计进货价
     */
    private BigDecimal purchasePrice;

}
