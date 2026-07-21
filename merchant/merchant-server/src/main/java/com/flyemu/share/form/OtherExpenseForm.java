package com.flyemu.share.form;

import com.flyemu.share.entity.fund.OtherExpense;
import com.flyemu.share.entity.fund.OtherExpenseItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OtherExpenseForm {
    private OtherExpense order;

    private List<OtherExpenseItem> itemList;
}
