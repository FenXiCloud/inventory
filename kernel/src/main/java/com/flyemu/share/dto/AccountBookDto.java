package com.flyemu.share.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;


@Getter
@Setter
public class AccountBookDto {

    private Long id;

    private String name;

    private LocalDate checkoutDate;

    private Boolean current;

    private Boolean enabled;

    private LocalDate startDate;

    private Integer warehouseId;

    private String warehouseName;

    private Long merchantId;
}
