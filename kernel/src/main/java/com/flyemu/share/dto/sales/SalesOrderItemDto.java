package com.flyemu.share.dto.sales;

import com.flyemu.share.dto.AuxiliaryUnitPrice;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesOrderItemDto {

    private Long id;

    @Comment("销售订单主表ID")
    private Long salesOrderId;

    @Comment("产品ID")
    private Long productId;
    @Comment("产品code")
    private String productCode;
    @Comment("产品name")
    private String productName;
    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("基本单位")
    private String unitName;

    @Comment("数量（以基本单位计）")
    private BigDecimal quantity;

    @Comment("出库数量")
    private BigDecimal quantityOut;

    @Comment("退货数量")
    private BigDecimal quantityReturn;

    @Comment("辅助单位ID(可为空)")
    private Long secondaryUnitId;

    @Comment("辅助单位名称")
    private String secondaryUnitName;

    @Comment("辅助单位数量 (可为空")
    private BigDecimal secondaryQuantity;

    @Comment("销售单位单价（业务单价 = 基本单价 × 换算率）")
    private BigDecimal secondaryPrice;

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

    @Comment("可用单位下拉（服务端回填，基本单位在前）")
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;
}
