package com.flyemu.share.form;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 产品价格资料 - 单元格保存
 * field = purchasePrice | levelPrice
 */
@Data
public class ProductPriceCellForm {

    @NotNull
    private Long productId;

    /**
     * purchasePrice：默认采购价；levelPrice：客户等级价
     */
    @NotNull
    private String field;

    /** field=levelPrice 时必填 */
    private Long customerLevelId;

    @NotNull
    private BigDecimal price;
}
