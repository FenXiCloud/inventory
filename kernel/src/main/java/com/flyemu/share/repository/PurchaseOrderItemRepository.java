package com.flyemu.share.repository;

import com.flyemu.share.entity.purchase.PurchaseOrderItem;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface PurchaseOrderItemRepository extends JpaRepositoryImplementation<PurchaseOrderItem, Long> {

}
