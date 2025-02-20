package com.flyemu.share.dto;

import com.flyemu.share.entity.inventory.CostAdjustment;
import lombok.Data;

@Data
public class CostAdjustmentDto extends CostAdjustment {

    private String createdByName;
}
