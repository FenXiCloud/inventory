package com.flyemu.share.enums;

/**
 * 成本核算方法（与账套参数 costAccounting 对应：1=移动加权 2=先进先出）
 */
public enum CostingMethod {
    移动加权平均,
    先进先出;

    public static CostingMethod fromParam(Integer costAccounting) {
        if (costAccounting != null && costAccounting == 2) {
            return 先进先出;
        }
        return 移动加权平均;
    }
}
