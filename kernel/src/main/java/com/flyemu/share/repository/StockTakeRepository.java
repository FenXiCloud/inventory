package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.StockTake;
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
public interface StockTakeRepository extends JpaRepositoryImplementation<StockTake, Long> {

    @Query(value = "SELECT\n" +
            "\tjst.id AS id,\n" +
            "\tdate_format(jst.check_date, '%Y-%m-%d') AS checkDate,\n" +
            "\tjst.remarks AS remarks,\n" +
            "\tjst.warehouse_id AS mainWarehouseId,\n" +
            "\tjsti.id AS itemId,\n" +
            "\tjsti.actual_quantity AS actualQuantity,\n" +
            "\tjsti.system_quantity AS systemQuantity,\n" +
            "\tjsti.warehouse_id AS warehouseId,\n" +
            "\tjw.`name` AS warehouseName,\n" +
            "\tjp.`name` AS productName,\n" +
            "\tjp.`code` AS productCode,\n" +
            "\tjp.img_path AS productImgPath,\n" +
            "\tjp.specification AS productSpecification,\n" +
            "\tjpc.`name` AS productCategoryName,\n" +
            "\tju.id AS baseUnitId,\n" +
            "\tju.`name` AS productUnitName,\n" +
            "\tjsti.actual_quantity - jsti.system_quantity AS deficient,\n" +
            "\tja.`name` AS adminName,\n" +
            "\tjsti.difference_reason AS differenceReason \n" +
            "FROM\n" +
            "\tjxc_stock_take jst\n" +
            "\tLEFT JOIN jxc_stock_take_item jsti ON (jst.id = jsti.stock_take_id)\n" +
            "\tLEFT JOIN jxc_product jp ON (jp.id = jsti.product_id)\n" +
            "\tLEFT JOIN jxc_product_category jpc ON (jpc.id = jp.product_category_id)\n" +
            "\tLEFT JOIN jxc_warehouse jw ON (jw.id = jsti.warehouse_id)\n" +
            "\tLEFT JOIN jxc_unit ju ON (ju.id = jp.unit_id) \n" +
            "\tLEFT JOIN jxc_admin ja ON (ja.id = jst.created_by) \n" +
            "WHERE\n" +
            "\tjst.id = ?1", nativeQuery = true)
    List<Map<String, Object>> load(Long id);
}