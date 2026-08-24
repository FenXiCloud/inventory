package com.flyemu.share.form;

import com.flyemu.share.entity.purchase.PurchaseInboundItem;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 销售订单转采购入库 表单
 */
@Data
public class TransferToPurchaseForm {

    /** 销售订单ID */
    private Long salesOrderId;

    /** 供货商ID */
    private Long supplierId;

    /** 采购入库明细 */
    private List<PurchaseInboundItem> items;

    /** 预计到货日期 */
    private LocalDate expectedDeliveryDate;

    /** 备注 */
    private String remark;

    /** 是否自动审核（为空时使用系统参数默认值） */
    private Boolean autoAudit;

    /** 生成方式：草稿/待审核/已审核（为空时使用系统参数默认值） */
    private String orderStatus;
}
