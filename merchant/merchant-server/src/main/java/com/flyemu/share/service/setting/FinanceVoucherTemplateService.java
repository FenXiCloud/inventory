package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceVoucherTemplate;
import com.flyemu.share.entity.setting.QFinanceVoucherTemplate;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.FinanceVoucherTemplateRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


/**
 * @功能描述: 云财务凭证模板
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceVoucherTemplateService extends AbsService {

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

    public FinanceVoucherTemplate load(Long id) {
        return financeVoucherTemplateRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qFinanceVoucherTemplate.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qFinanceVoucherTemplate.accountBookId.eq(accountBookId));
            }
        }

    }
}
