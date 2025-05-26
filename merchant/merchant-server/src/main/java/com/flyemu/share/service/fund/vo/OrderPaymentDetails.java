package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.OrderPaymentCollection;
import com.flyemu.share.entity.fund.OrderPaymentItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OrderPaymentDetails {
    private OrderPaymentDetailsVO orderReceipt;
    private List<OrderPaymentCollection> collectionList;
    private List<OrderPaymentItem> itemList;
}
