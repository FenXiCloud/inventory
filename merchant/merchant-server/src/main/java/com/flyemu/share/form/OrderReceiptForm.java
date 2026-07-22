package com.flyemu.share.form;

import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.entity.fund.OrderReceiptCollection;
import com.flyemu.share.entity.fund.OrderReceiptItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OrderReceiptForm {

    private OrderReceipt orderReceipt;
    private List<OrderReceiptCollection> collectionList;
    private List<OrderReceiptItem> itemList;

}
