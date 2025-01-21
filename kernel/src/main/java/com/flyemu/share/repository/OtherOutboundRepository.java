package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.OtherOutbound;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;
import java.util.Map;


public interface OtherOutboundRepository extends JpaRepositoryImplementation<OtherOutbound, Long> {

    @Query(value = "SELECT\n" +
            "\tjoo.id AS id,\n" +
            "\tdate_format(joo.inbound_date, '%Y-%m-%d') AS inboundDate,\n" +
            "\tjoo.customer_id AS customerId,\n" +
            "\tjoo.outbound_type AS outboundType,\n" +
            "\tjoo.order_status AS orderStatus,\n" +
            "\tjooi.id AS itemId,\n" +
            "\tjp.id AS productId,\n" +
            "\tjp.img_path AS productUrl,\n" +
            "\tjp.`code` AS productCode,\n" +
            "\tjp.`name` AS productName,\n" +
            "\tjp.specification AS productSpecification,\n" +
            "\tjp.product_category_id AS productCategoryId,\n" +
            "\tjpc.`name` AS productCategoryName,\n" +
            "\tjp.unit_id AS productUnitId,\n" +
            "\tju.`name` AS productUnitName,\n" +
            "\tjooi.`warehouse_id` AS warehouseId,\n" +
            "\tjw.`name` AS warehouseName,\n" +
            "\tjooi.quantity AS quantity,\n" +
            "\tjooi.unit_price AS unitPrice,\n" +
            "\tjooi.subtotal AS subtotal,\n" +
            "\tja.`name` AS adminName,\n" +
            "\tjoo.remarks AS remarks\n" +
            "FROM\n" +
            "\tjxc_other_outbound joo\n" +
            "\tLEFT JOIN jxc_other_outbound_item jooi ON (joo.id = jooi.other_outbound_id)\n" +
            "\tLEFT JOIN jxc_product jp ON (jp.id = jooi.product_id)\n" +
            "\tLEFT JOIN jxc_product_category jpc ON (jpc.id = jp.product_category_id)\n" +
            "\tLEFT JOIN jxc_unit ju ON (ju.id = jp.unit_id) \n" +
            "\tLEFT JOIN jxc_admin ja ON (ja.id = joo.created_by) \n" +
            "\tLEFT JOIN jxc_warehouse jw ON (jw.id = jooi.warehouse_id)\n" +
            "WHERE\n" +
            "\tjoo.id = ?1", nativeQuery = true)
    List<Map<String, Object>> findOtherOutboundById(Long id);
}
