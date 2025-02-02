package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.OtherInbound;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;
import java.util.Map;


public interface OtherInboundRepository extends JpaRepositoryImplementation<OtherInbound, Long> {

    @Query(value = "SELECT\n" +
            "\tjoi.id AS id,\n" +
            "\tdate_format(joi.inbound_date, '%Y-%m-%d') AS inboundDate,\n" +
            "\tjoi.customer_id AS customerId,\n" +
            "\tjoi.supplier_id AS supplierId,\n" +
            "\tjoi.inbound_type AS inboundType,\n" +
            "\tjoi.order_status AS orderStatus,\n" +
            "\tjoii.id AS itemId,\n" +
            "\tjp.id AS productId,\n" +
            "\tjp.img_path AS productUrl,\n" +
            "\tjp.`code` AS productCode,\n" +
            "\tjp.`name` AS productName,\n" +
            "\tjp.specification AS productSpecification,\n" +
            "\tjp.product_category_id AS productCategoryId,\n" +
            "\tjpc.`name` AS productCategoryName,\n" +
            "\tjp.unit_id AS productUnitId,\n" +
            "\tju.`name` AS productUnitName,\n" +
            "\tjoii.`warehouse_id` AS warehouseId,\n" +
            "\tjw.`name` AS warehouseName,\n" +
            "\tjoii.quantity AS quantity,\n" +
            "\tjoii.unit_price AS unitPrice,\n" +
            "\tjoii.subtotal AS subtotal,\n" +
            "\tja.`name` AS adminName,\n" +
            "\tjoi.remarks AS remarks \n" +
            "FROM\n" +
            "\tjxc_other_inbound joi\n" +
            "\tLEFT JOIN jxc_other_inbound_item joii ON (joi.id = joii.other_inbound_id)\n" +
            "\tLEFT JOIN jxc_product jp ON (jp.id = joii.product_id)\n" +
            "\tLEFT JOIN jxc_product_category jpc ON (jpc.id = jp.product_category_id)\n" +
            "\tLEFT JOIN jxc_unit ju ON (ju.id = jp.unit_id)\n" +
            "\tLEFT JOIN jxc_admin ja ON (ja.id = joi.created_by)\n" +
            "\tLEFT JOIN jxc_warehouse jw ON (jw.id = joii.warehouse_id) \n" +
            "WHERE\n" +
            "\tjoi.id = ?1", nativeQuery = true)
    List<Map<String, Object>> findOtherInboundById(Long id);
}
