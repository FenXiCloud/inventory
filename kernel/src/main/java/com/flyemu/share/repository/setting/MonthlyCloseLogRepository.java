package com.flyemu.share.repository.setting;

import com.flyemu.share.entity.setting.MonthlyCloseLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyCloseLogRepository extends JpaRepository<MonthlyCloseLog, Long> {

    List<MonthlyCloseLog> findByMerchantIdAndAccountBookIdOrderByClosePeriodDesc(Long merchantId, Long accountBookId);

    Optional<MonthlyCloseLog> findByMerchantIdAndAccountBookIdAndClosePeriod(Long merchantId, Long accountBookId, LocalDate closePeriod);

    Optional<MonthlyCloseLog> findFirstByMerchantIdAndAccountBookIdAndStatusOrderByClosePeriodDesc(Long merchantId, Long accountBookId, Integer status);

    boolean existsByMerchantIdAndAccountBookIdAndClosePeriodAndStatus(Long merchantId, Long accountBookId, LocalDate closePeriod, Integer status);

    void deleteByMerchantIdAndAccountBookIdAndClosePeriod(Long merchantId, Long accountBookId, LocalDate closePeriod);
}
