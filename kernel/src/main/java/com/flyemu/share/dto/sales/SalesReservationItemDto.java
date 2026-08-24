package com.flyemu.share.dto.sales;

import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SalesReservationItemDto {

    private Long id;

    @Comment("销售预订主表ID")
    private Long salesReservationId;

    @Comment("产品ID")
    private Long productId;

    @Comment("产品编码")
    private String productCode;

    @Comment("产品名称")
    private String productName;

    @Comment("产品规格")
    private String specification;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("单位名称")
    private String unitName;

    @Comment("数量")
    private BigDecimal quantity;

    @Comment("已转进货数量")
    private BigDecimal quantityPurchased;

    @Comment("辅助单位ID")
    private Long secondaryUnitId;

    @Comment("辅助单位数量")
    private BigDecimal secondaryQuantity;

    @Comment("换算率")
    private BigDecimal conversionRate;

    @Comment("单价")
    private BigDecimal unitPrice;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountValue;

    @Comment("小计")
    private BigDecimal subtotal;

    @Comment("税率")
    private BigDecimal taxRate;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("仓库名称")
    private String warehouseName;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    private Long accountBookId;

    private Long merchantId;

    @Comment("备注")
    private String remark;
}
