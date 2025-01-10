package com.flyemu.share.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ProductDto {

    private Integer id;

    private String code;

    private String name;

    private String title;

    private BigDecimal purchasePrice;

    private Integer productCategoryId;

    private String productCategoryName;

    private String specification;

    private String imgPath;

    private Integer unitId;

    private String unitName;

    private Boolean enableMultiUnit;

    private Boolean enabled;

    private Date createDate;

    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    private Integer sort;

    private String remarks;
    
    private String pinyin;

    private Integer stockQuantity;

    private Integer alertQuantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
