package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.sales.SalesOutboundItem;
import lombok.Data;

import java.util.List;

@Data
public class SalesOutboundForm {

    private SalesOutbound salesOutbound;

    private List<SalesOutboundItem> salesOutboundItemList;

    /** 源销售订单 id（选单） */
    private List<Long> selectSalesOrderIdList;
}
