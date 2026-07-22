package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.entity.fund.OrderReceiptItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderReceiptQueryVO extends OrderReceipt {

    private List<OrderReceiptCollectionVO> collectionList;
    private List<OrderReceiptItem> itemList;
}
