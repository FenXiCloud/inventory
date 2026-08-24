package com.flyemu.share.dto.inventory;

import com.flyemu.share.enums.AssemblyOrderType;
import com.flyemu.share.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AssemblyOrderDto {

    private Long id;

    private String orderNo;

    private AssemblyOrderType orderType;

    private Long productId;

    private String productCode;

    private String productName;

    private String productSpecification;

    private String productUnitName;

    private BigDecimal quantity;

    private Long warehouseId;

    private String warehouseName;

    private String remarks;

    private BigDecimal costAmount;

    private OrderStatus orderStatus;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long accountBookId;

    private Long merchantId;
}
