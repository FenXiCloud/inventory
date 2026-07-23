package com.flyemu.share.dto.purchase;

import com.flyemu.share.enums.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)

public class PurchaseReturnDto {
    private Long id;

    /**
     * 单据编号
     */
    private String orderNo;

    /**
     * 采购入库主表ID
     */
    private Long purchaseInboundId;

    /**
     * 供货商ID
     */
    private Long supplierId;

    /**
     * 下单日期
     */
    private LocalDate returnDate;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;
    /**
     * 采购数量合计
     */
    private BigDecimal secondarySum;

    /**
     * 退货金额
     */
    private BigDecimal refundTotalAmount;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 退款金额
     */
    private BigDecimal refundAmount;

//    /**
//     * 商户承担金额
//     */
//    private BigDecimal supplierAmount;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 订单状态
     */
    private OrderStatus orderStatus;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 审核人
     */
    private Long approvedBy;

    /**
     * 审核时间
     */
    private LocalDateTime approvedAt;

    private Long accountBookId;

    private Long merchantId;

    /**
     * 供货商名称
     */
    private String supplierName;

    /**
     * 创建人名称
     */
    private String createdName;

    /**
     * 审核人名称
     */
    private String approvedName;

    /**
     * 关联采购入库单号
     */
    private String purchaseInboundNos;
}
