package com.flyemu.share.service.fund.dto;

import com.flyemu.share.entity.fund.AccountTransfer;
import com.flyemu.share.entity.fund.AccountTransferItem;
import com.flyemu.share.entity.fund.OtherExpense;
import com.flyemu.share.entity.fund.OtherExpenseItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class AccountTransferDTO {
    private AccountTransfer order;

    private List<AccountTransferItem> itemList;
}
