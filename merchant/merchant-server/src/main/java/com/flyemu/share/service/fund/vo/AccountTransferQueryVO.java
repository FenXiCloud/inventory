package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.entity.fund.AccountTransferItem;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@JsonInclude
@EqualsAndHashCode(callSuper = true)
@Data
public class AccountTransferQueryVO extends AccountTransferDetailsVO {
    private List<AccountTransferItem> itemList;
}
