package com.flyemu.share.dto.sales;

import com.flyemu.share.enums.OrderStatus;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
    销售订单DTO
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class SalesOrderDto {

    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("客户ID")
    private Long customerId;

    @Comment("客户name")
    private String customerName;

    @Comment("下单日期")
    private LocalDate orderDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

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
    @Comment("创建人")
    private String createdName;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    private Long accountBookId;

    private Long merchantId;

    private List<SalesOrderItemDto> salesOrderItemList;

    @Comment("销售出库单id")
    private Long outOrderId;
    @Comment("销售出库单编号")
    private String outOrderNo;

    private List<String> outOrderNoList;

    @Comment("商品数量")
    private AtomicReference<BigDecimal> totalQuantity;

    @Comment("出库单状态 0初始化 1部分出库 2全部出库")
    private Integer status;

    @Comment("采购状态 0未采购 1部分采购 2已全部采购")
    private Integer purchaseStatus;

    @Comment("采购状态文本")
    private String purchaseStatusText;

    @Comment("关联的采购入库单ID列表")
    private String purchaseInIds;

    @Comment("关联的采购入库单编号")
    private String purchaseInOrderNos;
}
