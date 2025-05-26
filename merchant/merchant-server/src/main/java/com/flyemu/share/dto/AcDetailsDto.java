package com.flyemu.share.dto;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class AcDetailsDto {
    private String id;
    private String accountSetsId;
    private String categoryId;

    private String categoryName;

    private String categoryType;
    private String code;
    private String name;
    private String mnemonics;
    private String remark;
    private Boolean enable;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
