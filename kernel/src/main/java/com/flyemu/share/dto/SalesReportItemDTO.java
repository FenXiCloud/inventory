package com.flyemu.share.dto;

import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 销售明细报表DTO
 */
@Data
public class SalesReportItemDTO {

    private Long id;

    @Comment("销售出库主表ID")
    private Long salesOutboundId;

    @Comment("产品ID")
    private Long productId;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("数量（以基本单位计）")
    private Double quantity;

    @Comment("辅助单位ID(可为空)")
    private Long secondaryUnitId;

    @Comment("辅助单位数量 (可为空")
    private Double secondaryQuantity;

    @Comment("换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)")
    private BigDecimal conversionRate;

    @Comment("单价（以基本单位计）")
    private BigDecimal unitPrice;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountValue;

    @Comment("小计 (quantity * unitPrice * (1 - discount_value/100) 或 quantity * unitPrice - discount_value，根据折扣类型计算)")
    private BigDecimal subtotal;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("仓库")
    private String warehouseName;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    private Long accountBookId;

    private Long merchantId;


    @Comment("产品code")
    private String productCode;
    @Comment("产品name")
    private String productName;
    @Comment("规格")
    private String specification;
    @Comment("单位")
    private String unitName;

    @Comment("备注")
    private String remark;

    @Comment("单据编号")
    private String orderNo;

    @Comment("客户分类id")
    private Long customerCategoryId;
    @Comment("客户name")
    private String customerName;
    @Comment("客户name")
    private String customerCode;
    @Comment("客户id")
    private Long customerId;

    @Comment("单据日期")
    private LocalDate orderDate;

    private String salesType;
}
