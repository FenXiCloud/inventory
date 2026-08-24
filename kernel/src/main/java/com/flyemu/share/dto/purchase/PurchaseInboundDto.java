package com.flyemu.share.dto.purchase;

import com.flyemu.share.enums.OrderStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)

public class PurchaseInboundDto {
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
     * 退货单主表ID
     */
    private Long purchaseReturnId;

    /**
     * 退货单orderNo
     */
    private String purchaseReturnOrderNo;

    /**
     * 供货商ID
     */
    private Long supplierId;

    /**
     * 入库日期
     */
    private LocalDate inboundDate;

    /**
     * 采购数量合计
     */
    private BigDecimal secondarySum;

    /**
     * 订单金额
     */
    private BigDecimal totalAmount;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 折后金额
     */
    private BigDecimal finalAmount;

    /**
     * 已核销金额
     */
    private BigDecimal verifiedAmount;

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
     * 关联采购单号
     */
    private String purchaseOrderNos;

    /**
     * 结算状态
     */
    private String settlementStatus;

    /**
     * 来源类型：以销定购/普通采购
     */
    private String sourceType;

    /**
     * 来源销售订单ID
     */
    private Long sourceSalesOrderId;
}
