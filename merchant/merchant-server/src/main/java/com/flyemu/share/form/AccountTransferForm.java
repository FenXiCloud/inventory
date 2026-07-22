package com.flyemu.share.form;

import com.flyemu.share.entity.fund.AccountTransfer;
import com.flyemu.share.entity.fund.AccountTransferItem;
import lombok.Data;

import java.util.List;

@Data
public class AccountTransferForm {
    private AccountTransfer order;

    private List<AccountTransferItem> itemList;
}
