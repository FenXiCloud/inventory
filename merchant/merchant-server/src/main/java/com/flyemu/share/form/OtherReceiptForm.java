package com.flyemu.share.form;

import com.flyemu.share.entity.fund.OtherReceipt;
import com.flyemu.share.entity.fund.OtherReceiptItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OtherReceiptForm {
    private OtherReceipt order;

    private List<OtherReceiptItem> itemList;
}
