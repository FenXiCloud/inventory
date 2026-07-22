package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.InventoryCostBatch;
import com.flyemu.share.enums.OperationType;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface InventoryCostBatchRepository extends JpaRepositoryImplementation<InventoryCostBatch, Long> {

    List<InventoryCostBatch> findByInboundOrderIdAndInboundOrderTypeAndMerchantIdAndAccountBookId(
            Long inboundOrderId, OperationType inboundOrderType, Long merchantId, Long accountBookId);

    List<InventoryCostBatch> findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
            Long productId, Long warehouseId, Long merchantId, Long accountBookId, Integer qtyRemain);
}
