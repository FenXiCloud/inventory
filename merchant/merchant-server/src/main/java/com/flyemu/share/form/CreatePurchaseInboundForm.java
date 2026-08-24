package com.flyemu.share.form;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 以销定购 - 生成采购入库单 表单（按供应商自动拆分）
 */
@Data
public class CreatePurchaseInboundForm {

    /** 销售订单ID */
    private Long saleOrderId;

    /** 预计到货日期 */
    private LocalDate expectedDeliveryDate;

    /** 备注 */
    private String remark;

    /** 是否自动审核（为空时使用系统参数默认值） */
    private Boolean autoAudit;

    /** 生成方式：草稿/待审核/已审核（为空时使用系统参数默认值） */
    private String orderStatus;

    /** 商品明细 */
    private List<Item> items;

    @Data
    public static class Item {
        /** 商品ID */
        private Long goodsId;
        /** 本次采购数量 */
        private BigDecimal quantity;
        /** 供应商ID */
        private Long supplierId;
        /** 采购单价 */
        private BigDecimal purchasePrice;
        /** 仓库ID */
        private Long warehouseId;
        /** 基础单位ID */
        private Long baseUnitId;
        /** 换算率 */
        private BigDecimal conversionRate;
    }
}
