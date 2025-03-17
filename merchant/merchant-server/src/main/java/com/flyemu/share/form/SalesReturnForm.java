package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.sales.SalesOutboundItem;
import com.flyemu.share.entity.sales.SalesReturn;
import com.flyemu.share.entity.sales.SalesReturnItem;
import com.flyemu.share.enums.OrderStatus;
import lombok.Data;

import java.util.List;

@Data
public class SalesReturnForm {

    private SalesReturn salesReturn;

    private List<SalesReturnItem> salesReturnItemList;

    private List<Long> orderIds;
    //选择的源单id-销售出库单
    private List<Long> selectSalesOutboundIdList;

    private OrderStatus orderStatus;
}
