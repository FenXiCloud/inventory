package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceVoucherTemplate;
import com.flyemu.share.entity.setting.QFinanceVoucherTemplate;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.FinanceVoucherTemplateRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceVoucherTemplateService extends BaseService {

    private final static QFinanceVoucherTemplate qFinanceVoucherTemplate = QFinanceVoucherTemplate.financeVoucherTemplate;

    private final FinanceVoucherTemplateRepository financeVoucherTemplateRepository;

    public List<FinanceVoucherTemplate> query(FinanceVoucherTemplateService.Query query) {
        return bqf.selectFrom(qFinanceVoucherTemplate)
                .where(query.builder)
                .orderBy(qFinanceVoucherTemplate.id.desc())
                .fetch();
    }

    @Transactional
    public FinanceVoucherTemplate save(FinanceVoucherTemplate financeVoucherTemplate, AccountDto accountDto) {
        if (financeVoucherTemplate.getId() != null) {
            //更新
            FinanceVoucherTemplate original = financeVoucherTemplateRepository.getById(financeVoucherTemplate.getId());
            BeanUtil.copyProperties(financeVoucherTemplate, original, CopyOptions.create().ignoreNullValue());
            original.setUpdatedAt(LocalDateTime.now());
            return financeVoucherTemplateRepository.save(original);
        }
        List<FinanceVoucherTemplate> byType = financeVoucherTemplateRepository.findByType(financeVoucherTemplate.getType());
        if (!byType.isEmpty()) {
            throw new ServiceException("已有对应类型凭证模板～");
        }
        financeVoucherTemplate.setCreatedAt(LocalDateTime.now());
        financeVoucherTemplate.setCreatedBy(accountDto.getAdminId());
        return financeVoucherTemplateRepository.save(financeVoucherTemplate);
    }

    public FinanceVoucherTemplate findByType(String type) {
        List<FinanceVoucherTemplate> byType = financeVoucherTemplateRepository.findByType(type);
        if (!byType.isEmpty()) {
            return byType.get(0);
        }
        return null;
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qFinanceVoucherTemplate)
                .where(qFinanceVoucherTemplate.id.eq(id).and(qFinanceVoucherTemplate.merchantId.eq(merchantId)).and(qFinanceVoucherTemplate.accountBookId.eq(accountBookId)))
                .execute();
    }

    public FinanceVoucherTemplate load(Long merchantId, Long id) {
        return jqf.selectFrom(qFinanceVoucherTemplate)
                .where(qFinanceVoucherTemplate.merchantId.eq(merchantId).and(qFinanceVoucherTemplate.id.eq(id)))
                .fetchOne();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qFinanceVoucherTemplate.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qFinanceVoucherTemplate.accountBookId, accountBookId);
        }

    }
}
