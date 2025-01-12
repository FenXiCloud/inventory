package com.flyemu.share.form;

import com.flyemu.share.entity.purchase.PurchaseOrder;
import com.flyemu.share.entity.purchase.PurchaseOrderItem;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseOrderForm {

    private PurchaseOrder purchaseOrder;

    private List<PurchaseOrderItem> purchaseOrderItemList;
}
