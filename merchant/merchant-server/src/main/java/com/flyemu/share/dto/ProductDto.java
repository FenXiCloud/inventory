package com.flyemu.share.dto;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class ProductDto {

    private Integer id;

    private String code;

    private String name;

    private String title;

    private Integer categoryId;

    private String categoryName;

    private String specification;

    private String imgPath;

    private Integer unitId;

    private String unitName;

    private Boolean enableMultiUnit;

    private Boolean enabled;

    private Date createDate;

    private List<AuxiliaryUnitPrice> multiUnit;

    private Integer sort;

    private String remarks;
    
    private String pinyin;
}
