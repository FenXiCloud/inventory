package com.flyemu.share.repository.setting;

import com.flyemu.share.entity.setting.DataBackup;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;

import java.util.List;
import java.util.Optional;

public interface DataBackupRepository extends JpaRepositoryImplementation<DataBackup, Long> {

    List<DataBackup> findByMerchantIdAndAccountBookIdOrderByCreatedAtDesc(Long merchantId, Long accountBookId);

    Optional<DataBackup> findByIdAndMerchantIdAndAccountBookId(Long id, Long merchantId, Long accountBookId);
}
