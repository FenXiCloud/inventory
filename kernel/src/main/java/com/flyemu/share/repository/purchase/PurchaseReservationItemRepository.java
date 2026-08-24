package com.flyemu.share.repository.purchase;

import com.flyemu.share.entity.purchase.PurchaseReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseReservationItemRepository extends JpaRepository<PurchaseReservationItem, Long> {

    List<PurchaseReservationItem> findByPurchaseReservationId(Long purchaseReservationId);
}
