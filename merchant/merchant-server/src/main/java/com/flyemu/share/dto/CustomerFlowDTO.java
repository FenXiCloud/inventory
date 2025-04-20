package com.flyemu.share.dto;

import com.flyemu.share.entity.fund.CustomerFlow;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CustomerFlowDTO implements Serializable {

    private Long id;

    @Comment("客户Id")
    private Long customerId;

    private String customerName;
    private String customerCode;

    @Comment("单据Id")
    private Long orderId;

    @Comment("操作类型")
    @Enumerated(EnumType.STRING)
    private CustomerFlow.CustomerFlowType customerFlowType;

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
