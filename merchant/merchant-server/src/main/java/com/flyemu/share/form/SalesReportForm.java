package com.flyemu.share.form;

import lombok.Data;

import java.time.LocalDate;
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

    //排行维度 PRODUCT / CUSTOMER
    private String rankingType;

    public LocalDate getStartDate() {
        return parseDate(start);
    }

    public LocalDate getEndDate() {
        return parseDate(end);
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.trim();
        if (text.length() >= 10) {
            text = text.substring(0, 10);
        }
        return LocalDate.parse(text);
    }
}
