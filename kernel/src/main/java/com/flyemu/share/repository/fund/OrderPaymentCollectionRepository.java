package com.flyemu.share.repository.fund;

import com.flyemu.share.entity.fund.OrderPaymentCollection;
import com.flyemu.share.entity.fund.OrderReceiptCollection;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

/**
 * 持久层
 *
 * @author shuaiqi
 * @since 2025-05-20 11:29:36
 */
public interface OrderPaymentCollectionRepository extends JpaRepositoryImplementation<OrderPaymentCollection, Integer> {

}

