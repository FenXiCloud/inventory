package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@JsonInclude()
@Data
@NoArgsConstructor
public class OrderReceiptDetailsVO {

    private String createName;
    private String updateName;
    private String approvedName;
    private String orderStaffName;

    private Long id;

    private Long customerId;

    private String customerName;

    private Integer orderType;

    private LocalDate orderDate;

    private String orderNo;

    private String documentSource;

    private BigDecimal discountAmount;

    private BigDecimal collectionAmount;

    private BigDecimal totalAmountsOwed;

    private BigDecimal verificationAmount;

    private BigDecimal advanceCollectionsAmount;

    private BigDecimal shouldVerificationAmount;

    private BigDecimal hasVerificationAmount;

    private BigDecimal notVerificationAmount;

    private Integer writeOffStatus;

    private OrderStatus orderStatus;

    private Long orderStaffId;

    private String orderStatusName;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

    private Long accountBookId;

    private Long merchantId;

}
