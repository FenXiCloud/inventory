package com.flyemu.share.service.basic;

import com.flyemu.share.entity.basic.CustomerLevelPrice;
import com.flyemu.share.entity.basic.QCustomerLevelPrice;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerLevelPriceService extends BaseService {

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
