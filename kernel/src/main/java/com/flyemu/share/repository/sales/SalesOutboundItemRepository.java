package com.flyemu.share.repository.sales;

import com.flyemu.share.entity.sales.SalesOutboundItem;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface SalesOutboundItemRepository extends JpaRepositoryImplementation<SalesOutboundItem, Long> {

    List<SalesOutboundItem> findBySalesOutboundId(Long salesOutboundId);
}
