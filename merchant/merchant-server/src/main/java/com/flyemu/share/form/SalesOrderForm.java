package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.enums.OrderStatus;
import lombok.Data;

import java.util.List;

/**
 * 销售订单表单
 */
@Data
public class SalesOrderForm {

    private SalesOrder salesOrder;

    private List<SalesOrderItem> salesOrderItemList;

    private List<Long> orderIds;

    private OrderStatus orderStatus;
}
