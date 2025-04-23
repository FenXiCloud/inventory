package com.flyemu.share.dto;

import com.flyemu.share.entity.fund.SupplierFlow;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SupplierFlowDTO implements Serializable {

    private Long id;

    @Comment("货商Id")
    private Long supplierId;
    private String supplierName;
    private String supplierCode;

    @Comment("单据Id")
    private Long orderId;

    @Comment("操作类型")
    @Enumerated(EnumType.STRING)
    private SupplierFlow.SupplierFlowType supplierFlowType;

    @Comment("金额")
    private BigDecimal amount;

    @Comment("交易前余额")
    private BigDecimal balanceBefore;

    @Comment("交易后余额")
    private BigDecimal balanceAfter;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
