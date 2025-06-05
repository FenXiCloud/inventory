package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.entity.fund.OrderPaymentCollection;
import com.flyemu.share.entity.fund.OrderPaymentItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */@JsonInclude()
@Data
public class OrderPaymentDetails {
    private OrderPaymentDetailsVO orderPayment;
    private List<OrderPaymentCollection> collectionList;
    private List<OrderPaymentItem> itemList;
}
