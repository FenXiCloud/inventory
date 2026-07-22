package com.flyemu.share.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class AuxiliaryUnitPrice {

    private Long unitId;

    private String unitName;

    private Double conversionRate = 1d; //换算值

    private BigDecimal unitPrice = BigDecimal.ZERO;

    public AuxiliaryUnitPrice(Long unitId, String unitName, Double conversionRate, BigDecimal unitPrice) {
        this.unitId = unitId;
        this.unitName = unitName;
        this.conversionRate = conversionRate;
        this.unitPrice = unitPrice;
    }
}
