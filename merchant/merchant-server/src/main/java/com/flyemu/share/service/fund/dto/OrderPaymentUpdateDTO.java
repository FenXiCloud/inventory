package com.flyemu.share.service.fund.dto;

import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.time.LocalDateTime;

/**
 *@author shuaiqi
 */
@Data
public class OrderPaymentUpdateDTO {

    private String id;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private Long createdBy;

    private Long updateBy;

    private LocalDateTime createdAt;

    private LocalDateTime updateAt;


    private Long approvedBy;


    private LocalDateTime approvedAt;

}
