package com.flyemu.share.dto;

import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
    销售退货单DTO
 */
@Data
public class SalesReturnDTO {

    @Id
    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("销售出库主表ID")
    private Long salesOutboundId;

    @Comment("客户ID")
    private Long customerId;
    @Comment("客户name")
    private String customerName;

    @Comment("退单日期")
    private LocalDate returnDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("客户承担金额")
    private BigDecimal customerAmount;

    @Comment("退款金额")
    private BigDecimal refundAmount;

    @Comment("已核销金额")
    private BigDecimal verifiedAmount;

    @Comment("付款金额")
    private BigDecimal paymentAmount;

    @Comment("账户ID")
    private Long accountId;

    @Comment("退单原因")
    private String returnReason;

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

    private List<SalesReturnItemDTO> salesReturnItemList;

    private String salesOutboundNos;
}
