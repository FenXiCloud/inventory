package com.flyemu.share.service.setting;

import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.enums.CostingMethod;
import com.flyemu.share.service.BaseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账套参数集中读取器：成本核算方法、负库存开关等业务参数的唯一读取入口。
 * <p>
 * 此前 CostingService / InventoryService / SalesOutboundService 各自维护了一份
 * "查参数判断开关" 的重复实现（口径易漂移），统一收敛到这里；
 * 作为叶子组件供各方注入，避免 CostingService ↔ InventoryService 的循环依赖。
 */
@Service
@Slf4j
@Transactional(readOnly = true)
public class AccountBookParamReader extends BaseService {

    private static final QAccountBookParameters Q_PARAMS = QAccountBookParameters.accountBookParameters;

    /**
     * 成本核算方法：每次实时查库（避免缓存导致切换后旧方法继续生效），缺省移动加权平均
     */
    public CostingMethod costingMethod(Long accountBookId) {
        if (accountBookId == null) {
            return CostingMethod.移动加权平均;
        }
        AccountBookParameters params = bqf.selectFrom(Q_PARAMS)
                .where(Q_PARAMS.accountBookId.eq(Math.toIntExact(accountBookId)))
                .fetchFirst();
        return CostingMethod.fromParam(params == null ? null : params.getCostAccounting());
    }

    /**
     * 可用库存是否允许为负：availableInventory 1=是 2=否（缺省否）
     */
    public boolean allowNegativeStock(Long accountBookId) {
        if (accountBookId == null) {
            return false;
        }
        AccountBookParameters params = bqf.selectFrom(Q_PARAMS)
                .where(Q_PARAMS.accountBookId.eq(Math.toIntExact(accountBookId)))
                .fetchFirst();
        return params != null && params.getAvailableInventory() != null && params.getAvailableInventory() == 1;
    }
}
