package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.InventoryItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface InventoryItemRepository extends JpaRepositoryImplementation<InventoryItem, Long> {

    @Query(value = """
                SELECT
                	id
                FROM
                	jxc_inventory_item t
                WHERE
                	t.operation_type = '期初余额'
                	AND (
                		EXISTS (SELECT 1 FROM jxc_inventory_item WHERE id = t.id AND date_format(created_at,'%Y-%m') = ?1)
                		OR (
                			NOT EXISTS (SELECT 1 FROM jxc_inventory_item WHERE id = t.id AND date_format(created_at,'%Y-%m') = ?1)
                			AND NOT EXISTS (SELECT 1 FROM jxc_inventory_item WHERE product_id = t.product_id AND date_format(created_at,'%Y-%m') = ?1)
                		)
                	)
            """, nativeQuery = true)
    List<Long> findInventoryItemQcByTime(String date);
}
