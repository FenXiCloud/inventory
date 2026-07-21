package com.flyemu.share.repository;

import com.flyemu.share.entity.inventory.InventoryCostConsume;
import com.flyemu.share.enums.OperationType;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;

public interface InventoryCostConsumeRepository extends JpaRepositoryImplementation<InventoryCostConsume, Long> {

    List<InventoryCostConsume> findByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
            Long outboundOrderId, OperationType outboundOrderType, Long merchantId, Long accountBookId);

    void deleteByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
            Long outboundOrderId, OperationType outboundOrderType, Long merchantId, Long accountBookId);
}
