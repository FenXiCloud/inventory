package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.entity.fund.AccountTransfer;
import com.flyemu.share.entity.fund.AccountTransferItem;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude()
@Data
public class AccountTransferDetails {
    private AccountTransferDetailsVO order;

    private List<AccountTransferItem> itemList;
}
