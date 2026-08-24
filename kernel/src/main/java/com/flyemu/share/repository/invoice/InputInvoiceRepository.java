package com.flyemu.share.repository.invoice;

import com.flyemu.share.entity.invoice.InputInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InputInvoiceRepository extends JpaRepository<InputInvoice, Long> {

    Page<InputInvoice> findAllByMerchantIdOrderByIssueDateDescIdDesc(Long merchantId, Pageable pageable);

    List<InputInvoice> findAllByMerchantId(Long merchantId);
}
