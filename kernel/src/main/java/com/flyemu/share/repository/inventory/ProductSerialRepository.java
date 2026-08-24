package com.flyemu.share.repository.inventory;

import com.flyemu.share.entity.inventory.ProductSerial;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;
import java.util.Optional;

public interface ProductSerialRepository extends JpaRepositoryImplementation<ProductSerial, Long> {

    List<ProductSerial> findByMerchantIdAndAccountBookIdOrderByIdDesc(Long merchantId, Long accountBookId);

    List<ProductSerial> findByMerchantIdAndAccountBookIdAndStatusOrderByIdDesc(Long merchantId, Long accountBookId, String status);

    List<ProductSerial> findByMerchantIdAndAccountBookIdAndProductIdOrderByIdDesc(Long merchantId, Long accountBookId, Long productId);

    List<ProductSerial> findByMerchantIdAndAccountBookIdAndSerialNumberContainingOrderByIdDesc(Long merchantId, Long accountBookId, String serialNumber);

    Optional<ProductSerial> findByMerchantIdAndSerialNumber(Long merchantId, String serialNumber);
}
