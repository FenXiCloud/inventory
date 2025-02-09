package com.flyemu.share.dto;

import com.flyemu.share.entity.inventory.InventoryTransfer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class InventoryTransferDto extends InventoryTransfer {

    private String FromWarehouseName;

    private String ToWarehouseName;

    private String createdByName;
}
