package com.flyemu.share.dto.sales;

import com.flyemu.share.enums.OrderStatus;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesReservationDto {

    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("客户ID")
    private Long customerId;

    @Comment("客户名称")
    private String customerName;

    @Comment("下单日期")
    private LocalDate orderDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("优惠率")
    private BigDecimal discountRate;

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

    @Comment("转进货状态")
    private Integer status;

    @Comment("转销售订单ID")
    private Long salesOrderId;

    private List<SalesReservationItemDto> salesReservationItemList;

    @Comment("商品数量")
    private BigDecimal totalQuantity;

    @Comment("状态文本")
    private String orderStatusText;

    @Comment("转进货状态文本")
    private String statusText;
}
