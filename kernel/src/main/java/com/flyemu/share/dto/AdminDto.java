package com.flyemu.share.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class AdminDto {

    private Integer id;

    private String name;

    private String mobile;

    private String username;

    private Boolean enabled;

    private Long merchantId;

    private Boolean systemDefault;

    private Long roleId;

    private Integer roleType;

    private String roleName;
}
