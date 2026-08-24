package com.flyemu.share.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ProductAttributeValue {

    private String name;

    private String value;

    public ProductAttributeValue(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
