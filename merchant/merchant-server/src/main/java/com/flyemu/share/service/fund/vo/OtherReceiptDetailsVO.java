package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *@author shuaiqi
 */
@JsonInclude()
@Data
public class OtherReceiptDetailsVO {
    private Long orderStaffId;
    private String orderStaffName;
    private String createName;
    private String updateName;
    private String approvedName;
    private Long updateBy;
    private Long id;
    private String settlementAccount;
    private Long settlementAccountId;
    private Long customerId;
    private LocalDate orderDate;
    private String orderNo;
    private String customerName;
    private BigDecimal arrearsAmount;
    private LocalDate expirationDate;
    private BigDecimal collectionAmount;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    private String remarks;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updateAt;
    private Long approvedBy;
    private LocalDateTime approvedAt;
    private Long accountBookId;
    private Long merchantId;
}
