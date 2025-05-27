package com.flyemu.share.service.fund.dto;

import com.flyemu.share.entity.fund.OtherIncome;
import com.flyemu.share.entity.fund.OtherIncomeItem;
import lombok.Data;

import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OtherIncomeDTO {
    private OtherIncome order;

    private List<OtherIncomeItem> itemList;
}
