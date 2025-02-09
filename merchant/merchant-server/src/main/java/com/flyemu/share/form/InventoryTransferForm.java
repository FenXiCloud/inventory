package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.InventoryTransfer;
import com.flyemu.share.entity.inventory.InventoryTransferItem;
import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.entity.inventory.OtherInboundItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class InventoryTransferForm implements Serializable {

    private InventoryTransfer inventoryTransfer;

    private List<InventoryTransferItem> inventoryTransferItems;
}
