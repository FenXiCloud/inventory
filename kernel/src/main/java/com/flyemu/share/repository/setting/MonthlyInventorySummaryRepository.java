package com.flyemu.share.repository.setting;

import com.flyemu.share.entity.setting.MonthlyInventorySummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MonthlyInventorySummaryRepository extends JpaRepository<MonthlyInventorySummary, Long> {

    List<MonthlyInventorySummary> findByMerchantIdAndAccountBookIdAndPeriodOrderByProductIdAscWarehouseIdAsc(
            Long merchantId, Long accountBookId, LocalDate period);

    List<MonthlyInventorySummary> findByMerchantIdAndAccountBookIdAndProductIdAndWarehouseIdOrderByPeriodDesc(
            Long merchantId, Long accountBookId, Long productId, Long warehouseId);

    boolean existsByMerchantIdAndAccountBookIdAndPeriod(Long merchantId, Long accountBookId, LocalDate period);

    void deleteByMerchantIdAndAccountBookIdAndPeriod(Long merchantId, Long accountBookId, LocalDate period);
}
