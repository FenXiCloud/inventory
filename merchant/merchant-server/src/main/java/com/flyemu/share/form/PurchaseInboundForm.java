package com.flyemu.share.form;

import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.entity.purchase.PurchaseInboundItem;
import lombok.Data;

import java.util.List;

@Data
public class PurchaseInboundForm {

    private PurchaseInbound purchaseInbound;

    private List<PurchaseInboundItem> purchaseInboundItemList;
}
