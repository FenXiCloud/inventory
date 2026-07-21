package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.OtherExpenseItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OtherExpenseDetails {
    private OtherExpenseDetailsVO order;

    private List<OtherExpenseItem> itemList;
}
