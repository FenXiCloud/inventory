package com.flyemu.share.dto;

import com.flyemu.share.entity.basic.CustomerLevelPrice;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class ProductDto {

    private Long id;

    private String code;

    private String name;

    private String title;

    private BigDecimal purchasePrice;

    private BigDecimal retailCustomerPrice;

    private Integer productCategoryId;

    private String productCategoryName;

    private String specification;

    private String brand;

    private String imgPath;

    private Integer unitId;

    private String unitName;

    private Boolean enableMultiUnit;

    private Boolean enabled;

    private Boolean enableBatch;

    private Boolean enableSerial;

    private Date createDate;

    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    private Integer sort;

    private String remarks;
    
    private String pinyin;

    private String barcode;

    private List<ProductAttributeValue> productAttributes;

    private Integer stockQuantity;

    private Integer alertQuantity;

    private Integer maxStockQuantity;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    List<CustomerLevelPrice> customerLevelPriceList;

    private BigDecimal lastSalePrice;

    private BigDecimal taxRate;

    private String goodsCode;
}
