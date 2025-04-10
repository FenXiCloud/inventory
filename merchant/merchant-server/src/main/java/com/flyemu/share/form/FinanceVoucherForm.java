package com.flyemu.share.form;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinanceVoucherForm implements Serializable {

    private Long orderId;

    private String orderName;

    private BigDecimal amount;

    private String type;

    private Long merchantId;

    private Long accountBookId;

    private LocalDate orderTime;

    private String remark;

    private Long productId;

    private Long customerId;

    private Long supplierId;
}
