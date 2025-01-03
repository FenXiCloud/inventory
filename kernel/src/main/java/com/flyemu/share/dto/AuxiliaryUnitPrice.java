package com.flyemu.share.dto;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @功能描述: 多规格单位价格
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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
