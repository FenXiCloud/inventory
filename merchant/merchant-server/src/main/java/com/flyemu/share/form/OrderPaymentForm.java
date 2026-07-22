package com.flyemu.share.form;

import com.flyemu.share.entity.fund.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OrderPaymentForm {

    private OrderPayment orderPayment;
    private List<OrderPaymentCollection> collectionList;
    private List<OrderPaymentItem> itemList;

}
