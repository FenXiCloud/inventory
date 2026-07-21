package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesReturn;
import com.flyemu.share.entity.sales.SalesReturnItem;
import lombok.Data;

import java.util.List;

@Data
public class SalesReturnForm {

    private SalesReturn salesReturn;

    private List<SalesReturnItem> salesReturnItemList;

    /** 源销售出库单 id（选单） */
    private List<Long> selectSalesOutboundIdList;
}
