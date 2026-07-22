package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.OtherReceiptItem;
import lombok.Data;

import java.util.List;

@Data
public class OtherReceiptDetails {
    private OtherReceiptDetailsVO order;

    private List<OtherReceiptItem> itemList;
}
