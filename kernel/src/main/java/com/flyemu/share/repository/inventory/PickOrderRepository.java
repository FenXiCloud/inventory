package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.PickOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PickOrderRepository extends JpaRepository<PickOrder, Long> {

    List<PickOrder> findByMerchantIdAndAccountBookIdOrderByCreatedAtDesc(Long merchantId, Long accountBookId);

    List<PickOrder> findByMerchantIdAndAccountBookIdAndSourceIdAndSourceType(
            Long merchantId, Long accountBookId, Long sourceId, String sourceType);

    List<PickOrder> findByMerchantIdAndAccountBookIdAndStatusOrderByCreatedAtDesc(
            Long merchantId, Long accountBookId, Integer status);

    PickOrder findByMerchantIdAndAccountBookIdAndOrderNo(Long merchantId, Long accountBookId, String orderNo);
}
