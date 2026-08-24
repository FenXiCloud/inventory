package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.AssemblyOrder;
import com.flyemu.share.entity.inventory.AssemblyOrderItem;
import lombok.Data;

import java.util.List;

@Data
public class AssemblyOrderForm {

    private AssemblyOrder assemblyOrder;

    private List<AssemblyOrderItem> assemblyOrderItemList;
}
