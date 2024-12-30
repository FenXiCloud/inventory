package com.flyemu.share.dto;

import com.flyemu.share.entity.basic.Customer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false)
public class CustomerDto extends Customer {
    private String categoryName;
    private String levelName;
}
