package com.flyemu.share.service.fund.dto;

import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderPaymentUpdateDTO {

    private String id;
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private Long createdBy;

    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Long approvedBy;

    private LocalDateTime approvedAt;

}
