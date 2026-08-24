package com.flyemu.share.dto.purchase;

import com.flyemu.share.enums.OrderStatus;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class PurchaseReservationDto {

    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("供货商ID")
    private Long supplierId;

    @Comment("供货商名称")
    private String supplierName;

    @Comment("下单日期")
    private LocalDate orderDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣率")
    private BigDecimal discountRate;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("备注")
    private String remarks;

    @Comment("订单状态")
    private OrderStatus orderStatus;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建人名称")
    private String createdName;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    private Long accountBookId;

    private Long merchantId;

    @Comment("转采购状态")
    private Integer status;

    @Comment("转采购订单ID")
    private Long purchaseOrderId;

    @Comment("来源销售预订号")
    private String sourceSalesReservationNo;

    private List<PurchaseReservationItemDto> purchaseReservationItemList;

    @Comment("商品数量")
    private BigDecimal totalQuantity;

    @Comment("状态文本")
    private String orderStatusText;

    @Comment("转采购状态文本")
    private String statusText;
}
