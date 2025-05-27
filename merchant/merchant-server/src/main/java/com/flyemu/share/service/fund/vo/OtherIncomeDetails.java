package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.OtherIncome;
import com.flyemu.share.entity.fund.OtherIncomeItem;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 *@author shuaiqi
 */
@Data
public class OtherIncomeDetails {
    private OtherIncomeDetailsVO order;

    private List<OtherIncomeItem> itemList;
}
