package com.flyemu.share.dto.sales;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 以销定购看板 - 待采购销售订单DTO（订单级）
 * 一行一个销售订单，展开查看商品明细
 */
@Data
public class PendingPurchaseOrderDto {

    private Long salesOrderId;
    private String salesOrderNo;
    private String customerName;
    private LocalDate orderDate;

    /** 商品数：该订单中“需要采购”的商品数量 */
    private Integer productCount;

    /** 待采购数量合计 */
    private BigDecimal pendingPurchaseQuantity;

    /** 建议供应商（去重拼接） */
    private List<String> supplierNames = new ArrayList<>();

    /** 采购状态：待处理/部分采购/已采购 */
    private String purchaseStatusText;

    /** 商品明细 */
    private List<PendingPurchaseItemDto> items = new ArrayList<>();
}
