package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.CostAdjustment;
import com.flyemu.share.entity.inventory.CostAdjustmentItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class CostAdjustmentForm implements Serializable {

    private CostAdjustment costAdjustment;

    private List<CostAdjustmentItem> costAdjustmentItems;
}
