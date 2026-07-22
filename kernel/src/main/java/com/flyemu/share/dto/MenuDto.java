package com.flyemu.share.dto;

import lombok.Data;

@Data
public class MenuDto {

    private String title;
    private String key;
    private String icon;
    private String group;
    private Long id;
    private Long parentId;
}
