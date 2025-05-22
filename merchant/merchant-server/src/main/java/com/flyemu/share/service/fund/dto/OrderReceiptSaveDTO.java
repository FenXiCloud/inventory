package com.flyemu.share.service.fund.dto;

import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.entity.fund.OrderReceiptCollection;
import com.flyemu.share.entity.fund.OrderReceiptItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author shuaiqi
 */
@Data
@NoArgsConstructor
public class OrderReceiptSaveDTO {

    private OrderReceipt orderReceipt;
    private List<OrderReceiptCollection> collectionList;
    private List<OrderReceiptItem> itemList;


}
