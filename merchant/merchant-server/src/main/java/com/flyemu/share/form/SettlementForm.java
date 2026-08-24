package com.flyemu.share.form;

import com.flyemu.share.entity.fund.Settlement;
import com.flyemu.share.entity.fund.SettlementItem;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class SettlementForm {

    private Settlement order;
    private List<SettlementItem> itemList;
}
