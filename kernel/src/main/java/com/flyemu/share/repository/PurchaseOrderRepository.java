package com.flyemu.share.repository;

import com.flyemu.share.entity.purchase.PurchaseOrder;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;


public interface PurchaseOrderRepository extends JpaRepositoryImplementation<PurchaseOrder, Long> {

}
