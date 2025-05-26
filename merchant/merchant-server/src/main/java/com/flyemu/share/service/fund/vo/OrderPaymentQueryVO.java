package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author shuaiqi
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OrderPaymentQueryVO extends OrderPayment {

    private List<OrderPaymentCollection> collectionList;
    private List<OrderPaymentItem> itemList;
}
