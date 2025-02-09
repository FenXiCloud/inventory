package com.flyemu.share.dto;

import com.flyemu.share.entity.inventory.StockTake;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockTakeDto extends StockTake {

    private String warehouseName;

    private String createdByName;

    private List<String> orderNos;
}
