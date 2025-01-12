package com.flyemu.share.dto;

import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)

public class PurchaserOrderDto {
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
    private LocalDate orderDate;

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
}
