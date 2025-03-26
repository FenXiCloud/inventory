package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.Inventory;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;


/**
 * @功能描述: Repository
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
public interface InventoryRepository extends JpaRepositoryImplementation<Inventory, Long> {

    String PRODUCT_SQL = """
            SELECT
                ji.current_quantity AS systemQuantity,
                ji.product_id AS productId,
                ji.warehouse_id AS warehouseId,
                jp.`name` AS productName,
                jp.`code` AS productCode,
                jw.`name` AS warehouseName,
                jp.specification AS productSpecification,
                jpc.`name` AS productCategoryName,
                jp.img_path AS productImgPath,
                ji.base_unit_id AS baseUnitId,
                ju.`name` AS productUnitName\s
             FROM
                jxc_inventory ji
                LEFT JOIN jxc_product jp ON (ji.product_id = jp.id)
                LEFT JOIN jxc_warehouse jw ON (jw.id = ji.warehouse_id)
                LEFT JOIN jxc_product_category jpc ON (jp.product_category_id = jpc.id)
                LEFT JOIN jxc_unit ju ON (ju.id = ji.base_unit_id)\s
             WHERE
                jp.id IS NOT NULL
                AND ji.account_book_id = ?1\s
                AND ji.merchant_id = ?2\s
            """;

    List<Inventory> findByProductId(Long productId);
}
