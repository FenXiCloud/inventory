package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesReturn;
import com.flyemu.share.entity.sales.SalesReturnItem;
import com.flyemu.share.enums.OrderStatus;
import lombok.Data;

import java.util.List;

@Data
public class SalesReportForm {

    private Long merchantId;
    private Long accountBookId;

    private String salesGroup;
    private String filter;
    private String start;
    private String end;
    private String customerId;

}
