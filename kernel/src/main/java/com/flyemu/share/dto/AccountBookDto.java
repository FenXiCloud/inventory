package com.flyemu.share.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Date;


@Getter
@Setter
public class AccountBookDto {

    private Long id;

    private String code;

    private String name;

    private Integer type;

    private String address;

    private String linkman;

    private String email;

    private String phone;

    private String poi;

    private LocalDateTime createDate;

    private Date startDate;

    private Boolean enabled;

    private Boolean current;

    private Integer areaId;

    private String areaName;

    private Integer levelId;

    private String levelName;

    private Integer warehouseId;

    private String warehouseName;

    private Long merchantId;

    private String extCode;
}
