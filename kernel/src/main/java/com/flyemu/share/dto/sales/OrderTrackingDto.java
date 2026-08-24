package com.flyemu.share.dto.sales;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 以销定购 - 订单追踪DTO
 */
@Data
public class OrderTrackingDto {

    /** 销售订单信息 */
    private Long salesOrderId;
    private String salesOrderNo;
    private String customerName;
    private LocalDate orderDate;
    private String orderStatus;
    private Integer purchaseStatus;
    private String purchaseStatusText;
    private Integer outboundStatus;

    /** 关联的采购入库单列表 */
    private List<PurchaseInboundBrief> purchaseInbounds;

    /** 关联的销售出库单列表 */
    private List<SalesOutboundBrief> salesOutbounds;

    @Data
    public static class PurchaseInboundBrief {
        private Long id;
        private String orderNo;
        private String supplierName;
        private LocalDate inboundDate;
        private String orderStatus;
        private BigDecimal finalAmount;
        private String sourceType;
    }

    @Data
    public static class SalesOutboundBrief {
        private Long id;
        private String orderNo;
        private LocalDate outboundDate;
        private String orderStatus;
    }
}
