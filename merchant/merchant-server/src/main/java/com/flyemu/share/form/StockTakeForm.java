package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.entity.inventory.StockTakeItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class StockTakeForm implements Serializable {

    private StockTake stockTake;

    private List<StockTakeItem> stockTakeItems;
}
