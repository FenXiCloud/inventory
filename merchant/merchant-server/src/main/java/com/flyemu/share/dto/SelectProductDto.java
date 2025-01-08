package com.flyemu.share.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;


@Data
public class SelectProductDto {
    private Long productId;
    private String imgPath;
    private String productCode;
    private String productName;
    private String title;
    private String spec;
    private String unitName;
    private Long unitId;
    private BigDecimal price;
    private String path;

    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    public void setTitle() {
        this.title = this.productCode + " " + this.productName;
    }

}
