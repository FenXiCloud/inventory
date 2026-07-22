package com.flyemu.share.dto;

import lombok.Value;

import java.io.Serializable;
@Value
public class RoleDto implements Serializable {
    Integer id;
    String name;
    Boolean systemDefault;
    Long merchantId;
}
