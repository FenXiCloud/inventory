package com.flyemu.share.service.basic;

import com.flyemu.share.entity.basic.CustomerLevelPrice;
import com.flyemu.share.entity.basic.QCustomerLevelPrice;
import com.flyemu.share.service.AbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @功能描述: 客户等级价格管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerLevelPriceService extends AbsService {

    private final static QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;

    /**
     * 根据产品id和等级id
     *
     * @param productId       产品id
     * @param customerLevelId 等级id
     * @return level price
     */
    public CustomerLevelPrice findByProductIdAndCustomerLevelId(Long productId, Long customerLevelId) {
        return jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(productId))
                .where(qCustomerLevelPrice.customerLevelId.eq(customerLevelId)).fetchOne();
    }
}
