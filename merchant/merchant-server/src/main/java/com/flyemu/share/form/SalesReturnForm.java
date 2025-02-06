package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.sales.SalesOutboundItem;
import com.flyemu.share.entity.sales.SalesReturn;
import com.flyemu.share.entity.sales.SalesReturnItem;
import lombok.Data;

import java.util.List;

@Data
public class SalesReturnForm {

    private SalesReturn salesReturn;

    private List<SalesReturnItem> salesReturnItemList;

    private List<Long> orderIds;
    //选择的源单id
    private List<Long> selectSalesOrderIdList;
}
