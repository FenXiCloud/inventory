package com.flyemu.share.repository.sales;

import com.flyemu.share.entity.sales.SalesReservationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SalesReservationItemRepository extends JpaRepository<SalesReservationItem, Long> {

    List<SalesReservationItem> findBySalesReservationId(Long salesReservationId);
}
