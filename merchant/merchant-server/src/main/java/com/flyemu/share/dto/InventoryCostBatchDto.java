package com.flyemu.share.dto;

import com.flyemu.share.enums.OperationType;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InventoryCostBatchDto {
    private Long id;
    private String batchNo;
    private Long productId;
    private String productCode;
    private String productName;
    private Long warehouseId;
    private String warehouseName;
    private LocalDate inboundDate;
    private Long inboundOrderId;
    private OperationType inboundOrderType;
    private Long inboundItemId;
    private Integer qtyIn;
    private Integer qtyRemain;
    private BigDecimal unitCost;
    /** 入库金额（= 入库数量 × 单位成本，先进先出登记用） */
    private BigDecimal amountIn;
    private BigDecimal totalCostRemain;
    private Long supplierId;
    private String supplierName;
    private Boolean closed;
    private LocalDateTime createdAt;
    private Long accountBookId;
    private Long merchantId;
}
