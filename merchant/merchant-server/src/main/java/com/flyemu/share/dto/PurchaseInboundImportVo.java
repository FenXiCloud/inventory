package com.flyemu.share.dto;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;

@Data
public class PurchaseInboundImportVo {

    @Comment("供应商编码")
    @Alias("供应商编码")
    private String supplierCode;

    @Comment("供应商名称")
    @Alias("供应商名称")
    private String supplierName;

    @Comment("入库日期")
    @Alias("入库日期")
    private String inboundDate;

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
