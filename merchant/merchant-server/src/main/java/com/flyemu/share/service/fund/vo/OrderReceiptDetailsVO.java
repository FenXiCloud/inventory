package com.flyemu.share.service.fund.vo;

import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.entity.fund.OrderReceiptCollection;
import com.flyemu.share.entity.fund.OrderReceiptItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author shuaiqi
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class OrderReceiptDetailsVO extends OrderReceipt {

    private String createName;
    private String updateName;
    private String approvedName;

}
