package com.flyemu.share.dto;

import lombok.Data;

import java.time.LocalDateTime;


@Data
public class AcDetailsDto {
    private Long id;
    private Long accountSetsId;
    private Long categoryId;

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
