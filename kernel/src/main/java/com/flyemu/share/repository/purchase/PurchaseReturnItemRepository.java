package com.flyemu.share.repository.purchase;

import com.flyemu.share.entity.purchase.PurchaseReturnItem;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface PurchaseReturnItemRepository extends JpaRepositoryImplementation<PurchaseReturnItem, Long> {

    List<PurchaseReturnItem> findByPurchaseReturnId(Long purchaseReturnId);
}
