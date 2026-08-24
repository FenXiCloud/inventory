package com.flyemu.share.repository.sales;

import com.flyemu.share.entity.sales.SalesReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SalesReservationRepository extends JpaRepository<SalesReservation, Long> {
}
