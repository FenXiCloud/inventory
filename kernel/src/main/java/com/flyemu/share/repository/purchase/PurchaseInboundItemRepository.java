package com.flyemu.share.repository.purchase;

import com.flyemu.share.entity.purchase.PurchaseInboundItem;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface PurchaseInboundItemRepository extends JpaRepositoryImplementation<PurchaseInboundItem, Long> {

    List<PurchaseInboundItem> findByPurchaseInboundId(Long purchaseInboundId);
}
