package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 *@author shuaiqi
 */
@JsonInclude()
@Data
public class AccountTransferDetailsVO {
    private String createName;
    private String updateName;
    private String approvedName;
    private Long id;
    private Long updateBy;
    private LocalDateTime updateAt;
    private LocalDate orderDate;

    private String orderNo;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;
}
