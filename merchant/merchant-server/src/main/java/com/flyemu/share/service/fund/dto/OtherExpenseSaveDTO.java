package com.flyemu.share.service.fund.dto;

import com.flyemu.share.entity.fund.OtherExpense;
import com.flyemu.share.entity.fund.OtherExpenseItem;
import com.flyemu.share.entity.fund.OtherIncome;
import com.flyemu.share.entity.fund.OtherIncomeItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OtherExpenseSaveDTO {
    private OtherExpense order;

    private List<OtherExpenseItem> itemList;
}
