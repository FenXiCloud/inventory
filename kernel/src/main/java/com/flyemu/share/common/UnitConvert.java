package com.flyemu.share.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 多单位换算统一助手。
 *
 * <p>约定：单据行的"基本单位"为库存/成本口径单位（最小单位），"采购/业务单位"为录入单位。
 * <ul>
 *   <li>基本数量 = 业务数量 × 换算率（1箱 = 12个 ⇒ 换算率 12）</li>
 *   <li>基本单价 = 业务单价 ÷ 换算率（库存成本以基本单位计）</li>
 * </ul>
 * 采购订单 / 采购入库 / 采购退货 / 销售等后续单据共用，避免各处内联换算不一致。
 */
public final class UnitConvert {

    /** 单价保留小数位（与既有 divide(…, 2) 一致） */
    public static final int PRICE_SCALE = 2;
    /** 数量保留小数位（与实体列 scale=4 一致） */
    public static final int QTY_SCALE = 4;

    private UnitConvert() {
    }

    /** 换算率归一化：null 或 0 一律视为 1 */
    public static BigDecimal rate(BigDecimal r) {
        return (r == null || r.signum() == 0) ? BigDecimal.ONE : r;
    }

    public static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 基本数量 = 业务数量 × 换算率 */
    public static BigDecimal toBaseQty(BigDecimal secondaryQty, BigDecimal r) {
        return nvl(secondaryQty).multiply(rate(r));
    }

    /** 业务数量 = 基本数量 ÷ 换算率 */
    public static BigDecimal toSecondaryQty(BigDecimal baseQty, BigDecimal r) {
        return nvl(baseQty).divide(rate(r), QTY_SCALE, RoundingMode.HALF_UP);
    }

    /** 基本单价 = 业务单价 ÷ 换算率 */
    public static BigDecimal unitPrice(BigDecimal secondaryPrice, BigDecimal r) {
        return nvl(secondaryPrice).divide(rate(r), PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /** 业务单价 = 基本单价 × 换算率 */
    public static BigDecimal secondaryPrice(BigDecimal unitPrice, BigDecimal r) {
        return nvl(unitPrice).multiply(rate(r)).setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    /** 基本数量能否被换算率整除（即是否可表示为整数个业务单位） */
    public static boolean isWholeSecondary(BigDecimal baseQty, BigDecimal r) {
        BigDecimal rate = rate(r);
        return rate.compareTo(BigDecimal.ZERO) != 0
                && nvl(baseQty).remainder(rate).compareTo(BigDecimal.ZERO) == 0;
    }
}
