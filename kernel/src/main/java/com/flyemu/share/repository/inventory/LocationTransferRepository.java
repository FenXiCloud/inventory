package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.LocationTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationTransferRepository extends JpaRepository<LocationTransfer, Long> {

    List<LocationTransfer> findByMerchantIdAndAccountBookIdOrderByCreatedAtDesc(Long merchantId, Long accountBookId);

    List<LocationTransfer> findByMerchantIdAndAccountBookIdAndOrderStatusOrderByCreatedAtDesc(
            Long merchantId, Long accountBookId, String orderStatus);

    LocationTransfer findByMerchantIdAndAccountBookIdAndOrderNo(Long merchantId, Long accountBookId, String orderNo);

    boolean existsByMerchantIdAndAccountBookIdAndOrderNo(Long merchantId, Long accountBookId, String orderNo);
}
