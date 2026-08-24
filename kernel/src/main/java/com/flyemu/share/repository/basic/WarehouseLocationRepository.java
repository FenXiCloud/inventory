package com.flyemu.share.repository.basic;

import com.flyemu.share.entity.basic.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {

    List<WarehouseLocation> findByMerchantIdAndAccountBookIdOrderByWarehouseIdAscCodeAsc(
            Long merchantId, Long accountBookId);

    List<WarehouseLocation> findByMerchantIdAndAccountBookIdAndWarehouseIdOrderByCodeAsc(
            Long merchantId, Long accountBookId, Long warehouseId);

    List<WarehouseLocation> findByMerchantIdAndAccountBookIdAndTypeOrderByCodeAsc(
            Long merchantId, Long accountBookId, String type);

    List<WarehouseLocation> findByMerchantIdAndAccountBookIdAndWarehouseIdAndTypeOrderByCodeAsc(
            Long merchantId, Long accountBookId, Long warehouseId, String type);

    WarehouseLocation findByMerchantIdAndAccountBookIdAndWarehouseIdAndCode(
            Long merchantId, Long accountBookId, Long warehouseId, String code);

    boolean existsByMerchantIdAndAccountBookIdAndWarehouseIdAndCode(
            Long merchantId, Long accountBookId, Long warehouseId, String code);
}
