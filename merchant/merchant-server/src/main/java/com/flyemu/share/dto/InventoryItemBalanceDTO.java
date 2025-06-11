package com.flyemu.share.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 库存余额对象
 */
@Data
public class InventoryItemBalanceDTO implements Serializable {

    private Long inventoryItemId;

    private Long productId;

    private Long warehouseId;

    private String warehouseName;

    private Integer currentQuantity;

    private BigDecimal totalCost;

    private BigDecimal averageCost;

    private LocalDateTime createdAt;

    private Date inventoryDate;

}
