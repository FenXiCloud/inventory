package com.flyemu.share.dto.invoice;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InvoiceRequest {
    private String buyerName;
    private String buyerTaxNo;
    private String remark;
    private String payee;
    private String reviewer;
    private List<ItemRequest> items;
    private Long sourceId;
    private String sourceType;

    @Data
    public static class ItemRequest {
        private String goodsName;
        private BigDecimal quantity;
        private BigDecimal unitPrice;
        private BigDecimal taxRate;
    }
}
