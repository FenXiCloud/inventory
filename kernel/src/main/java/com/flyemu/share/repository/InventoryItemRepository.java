package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.InventoryItem;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;


/**
 * @功能描述: Repository
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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
