package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.InventoryTransfer;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;
import java.util.Map;


/**
 * @功能描述: Repository
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
public interface InventoryTransferRepository extends JpaRepositoryImplementation<InventoryTransfer, Long> {

    @Query(value = "SELECT\n" +
            "\tjit.id AS id,\n" +
            "\tdate_format(jit.transfer_date, '%Y-%m-%d') AS transferDate,\n" +
            "\tjit.order_status AS orderStatus,\n" +
            "\tjiti.id AS itemId,\n" +
            "\tjp.id AS productId,\n" +
            "\tjp.img_path AS productUrl,\n" +
            "\tjp.`code` AS productCode,\n" +
            "\tjp.`name` AS productName,\n" +
            "\tjp.specification AS productSpecification,\n" +
            "\tjp.product_category_id AS productCategoryId,\n" +
            "\tjpc.`name` AS productCategoryName,\n" +
            "\tjp.unit_id AS productUnitId,\n" +
            "\tju.`name` AS productUnitName,\n" +
            "\tjit.`from_warehouse_id` AS fromWarehouseId,\n" +
            "\tjit.`to_warehouse_id` AS toWarehouseId,\n" +
            "\tjwf.`name` AS fromWarehouseName,\n" +
            "\tjwt.`name` AS toWarehouseName,\n" +
            "\tjiti.quantity AS quantity,\n" +
            "\tja.`name` AS adminName,\n" +
            "\tSUM(IF(jiti.product_id = jif.product_id, jif.current_quantity, NULL)) AS warehouseQuantity,\n" +
            "\tSUM(jif.current_quantity) AS warehouseTotal,\n" +
            "\tjit.remarks AS remarks \n" +
            "FROM\n" +
            "\tjxc_inventory_transfer jit\n" +
            "\tLEFT JOIN jxc_inventory_transfer_item jiti ON (jiti.inventory_transfer_id = jit.id)\n" +
            "\tLEFT JOIN jxc_inventory jif ON (jif.warehouse_id = jit.from_warehouse_id)\n" +
            "\tLEFT JOIN jxc_product jp ON (jp.id = jiti.product_id)\n" +
            "\tLEFT JOIN jxc_product_category jpc ON (jpc.id = jp.product_category_id)\n" +
            "\tLEFT JOIN jxc_unit ju ON (ju.id = jp.unit_id)\n" +
            "\tLEFT JOIN jxc_admin ja ON (ja.id = jit.created_by)\n" +
            "\tLEFT JOIN jxc_warehouse jwf ON (jwf.id = jit.from_warehouse_id)\n" +
            "\tLEFT JOIN jxc_warehouse jwt ON (jwt.id = jit.to_warehouse_id) \n" +
            "WHERE\n" +
            "\tjit.id = ?1 \n" +
            "GROUP BY\n" +
            "\tjiti.id", nativeQuery = true)
    List<Map<String, Object>> findInventoryTransferById(Long id);
}
