package com.flyemu.share.service.invoice;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.invoice.InvoiceSellerConfig;
import com.flyemu.share.repository.invoice.InvoiceSellerConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvoiceSellerConfigService {

    private final InvoiceSellerConfigRepository repository;

    @Transactional(readOnly = true)
    public InvoiceSellerConfig get(Long merchantId) {
        return repository.findTopByMerchantIdOrderByIdDesc(merchantId).orElse(null);
    }

    @Transactional
    public InvoiceSellerConfig save(InvoiceSellerConfig config, Long merchantId, Long accountBookId) {
        config.setMerchantId(merchantId);
        config.setAccountBookId(accountBookId);
        if (config.getId() != null) {
            InvoiceSellerConfig original = repository.findById(config.getId()).orElse(new InvoiceSellerConfig());
            BeanUtil.copyProperties(config, original, CopyOptions.create().ignoreNullValue());
            original.setMerchantId(merchantId);
            original.setAccountBookId(accountBookId);
            return repository.save(original);
        }
        InvoiceSellerConfig existing = get(merchantId);
        if (existing != null) {
            config.setId(existing.getId());
            BeanUtil.copyProperties(config, existing, CopyOptions.create().ignoreNullValue());
            existing.setMerchantId(merchantId);
            existing.setAccountBookId(accountBookId);
            return repository.save(existing);
        }
        return repository.save(config);
    }
}
