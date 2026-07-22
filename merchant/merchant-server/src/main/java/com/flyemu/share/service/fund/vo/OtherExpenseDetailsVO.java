package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude()
@Data
public class OtherExpenseDetailsVO {
    private Long orderStaffId;
    private String orderStaffName;
    private String createName;
    private String updateName;
    private String approvedName;
    private Long id;
    private String settlementAccount;
    private Long settlementAccountId;
    private Long supplierId;
    private LocalDate orderDate;
    private String orderNo;
    private String supplierName;
    private BigDecimal arrearsAmount;
    private LocalDate expirationDate;
    private BigDecimal collectionAmount;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private String remarks;
    private Long createdBy;

    private LocalDateTime createdAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long accountBookId;

    private Long merchantId;
}
