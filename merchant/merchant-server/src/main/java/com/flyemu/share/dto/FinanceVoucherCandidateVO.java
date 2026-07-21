package com.flyemu.share.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceVoucherCandidateVO {

    private Long id;

    private Long orderId;

    private String orderNo;

    private LocalDate orderDate;

    private String documentType;

    private BigDecimal amount;

    private String customerName;

    private String supplierName;

    private String createName;

    private String voucherCode;

    private Long voucherId;

    private Long financeVoucherId;

    private Long customerId;

    private Long supplierId;

    private Long productId;

    private String orderName;
}
