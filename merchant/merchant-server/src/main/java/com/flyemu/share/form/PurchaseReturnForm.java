package com.flyemu.share.form;

import com.flyemu.share.entity.purchase.PurchaseReturn;
import com.flyemu.share.entity.purchase.PurchaseReturnItem;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseReturnForm {

    private PurchaseReturn purchaseReturn;

    private List<PurchaseReturnItem> purchaseReturnItemList;
}
