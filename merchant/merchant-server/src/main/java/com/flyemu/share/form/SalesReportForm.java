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

    //分组条件组合
    private String salesGroup;
    private String filter;
    private String start;
    private String end;
    private String customerId;
    private String warehouseId;
    private String productId;
    private String productCategoryId;

    private List<Long> customerIds;
    private List<Long> warehouseIds;
    private List<Long> productIds;
    private List<Long> productCategoryIds;
    private List<Long> customerCategoryIds;

    //销售类型 all out return
    private String salesType;

}
