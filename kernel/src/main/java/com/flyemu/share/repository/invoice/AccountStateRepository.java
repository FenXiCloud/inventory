package com.flyemu.share.repository.invoice;

import com.flyemu.share.entity.invoice.AccountState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountStateRepository extends JpaRepository<AccountState, Long> {

    /** 取指定商户最新一条状态记录 */
    Optional<AccountState> findTopByMerchantIdOrderByIdDesc(Long merchantId);
}
