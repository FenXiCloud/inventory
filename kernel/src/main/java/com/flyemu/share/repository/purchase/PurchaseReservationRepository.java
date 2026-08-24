package com.flyemu.share.repository.purchase;

import com.flyemu.share.entity.purchase.PurchaseReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseReservationRepository extends JpaRepository<PurchaseReservation, Long> {
}
