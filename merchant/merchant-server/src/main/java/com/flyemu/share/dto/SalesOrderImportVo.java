package com.flyemu.share.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Data
public class SalesOrderImportVo {

    @Comment("客户编码")
    @Alias("客户编码")
    private String customerCode;

    @Comment("客户名称")
    @Alias("客户名称")
    private String customerName;

    @Comment("单据日期")
    @Alias("单据日期")
    private String orderDate;

    @Comment("产品编码")
    @Alias("产品编码")
    private String productCode;

    @Comment("产品名称")
    @Alias("产品名称")
    private String productName;

    @Comment("数量")
    @Alias("数量")
    private BigDecimal quantity;

    @Comment("单价")
    @Alias("单价")
    private BigDecimal unitPrice;

    @Comment("仓库名称")
    @Alias("仓库")
    private String warehouseName;

    @Comment("折扣率")
    @Alias("折扣率(%)")
    private BigDecimal discountRate;

    @Comment("单据编号")
    @Alias("单据编号")
    private String orderNo;

    @Comment("备注")
    @Alias("备注")
    private String remarks;
}
