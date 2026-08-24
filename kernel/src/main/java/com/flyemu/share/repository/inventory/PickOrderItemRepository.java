package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.PickOrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickOrderItemRepository extends JpaRepository<PickOrderItem, Long> {

    List<PickOrderItem> findByPickOrderIdOrderByLocationCodeAsc(Long pickOrderId);

    void deleteByPickOrderId(Long pickOrderId);
}
