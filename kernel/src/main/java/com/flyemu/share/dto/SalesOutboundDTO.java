package com.flyemu.share.dto;

import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 销售出库单
 */
@Data
public class SalesOutboundDTO {

    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("销售订单ID")
    private String orderId;

    @Comment("客户ID")
    private Long customerId;
    @Comment("客户name")
    private String customerName;

    @Comment("出库日期")
    private LocalDate outboundDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("已核销金额")
    private BigDecimal verifiedAmount;

    @Comment("收款金额")
    private BigDecimal collectionAmount;

    @Comment("账户ID")
    private Long accountId;

    @Comment("备注")
    private String remarks;

    @Comment("订单状态")
    @Column(nullable = false,length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
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

    private List<SalesOutboundItemDTO> salesOutboundItemList;

    private String salesOrderNos;

    @Comment("商品数量")
    private AtomicReference<Double> totalQuantity;
}
