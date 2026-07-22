package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.setting.PrintTemplate;
import com.flyemu.share.entity.setting.QPrintTemplate;
import com.flyemu.share.repository.setting.PrintTemplateRepository;
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
public class PrintTemplateService extends BaseService {

    private final static QPrintTemplate qPrintTemplate = QPrintTemplate.printTemplate;

    private final PrintTemplateRepository printTemplateRepository;

    public List<PrintTemplate> query(Query query) {
        return bqf.selectFrom(qPrintTemplate)
                .where(query.builder)
                .orderBy(qPrintTemplate.id.desc())
                .fetch();
    }

    @Transactional
    public PrintTemplate save(PrintTemplate printTemplate) {
        if (printTemplate.getId() != null) {
            PrintTemplate original = printTemplateRepository.getById(printTemplate.getId());
            BeanUtil.copyProperties(printTemplate, original, CopyOptions.create().ignoreNullValue());
            if (Boolean.TRUE.equals(original.getSystemDefault())
                    && original.getDocumentType() != null) {
                BooleanBuilder clearDefault = new BooleanBuilder();
                clearDefault.and(qPrintTemplate.merchantId.eq(original.getMerchantId()))
                        .and(qPrintTemplate.accountBookId.eq(original.getAccountBookId()))
                        .and(qPrintTemplate.documentType.eq(original.getDocumentType()))
                        .and(qPrintTemplate.id.ne(original.getId()));
                jqf.update(qPrintTemplate)
                        .set(qPrintTemplate.systemDefault, false)
                        .where(clearDefault)
                        .execute();
            }
            return printTemplateRepository.save(original);
        }

        if (Boolean.TRUE.equals(printTemplate.getSystemDefault())
                && printTemplate.getDocumentType() != null
                && printTemplate.getMerchantId() != null
                && printTemplate.getAccountBookId() != null) {
            BooleanBuilder clearDefault = new BooleanBuilder();
            clearDefault.and(qPrintTemplate.merchantId.eq(printTemplate.getMerchantId()))
                    .and(qPrintTemplate.accountBookId.eq(printTemplate.getAccountBookId()))
                    .and(qPrintTemplate.documentType.eq(printTemplate.getDocumentType()));
            jqf.update(qPrintTemplate)
                    .set(qPrintTemplate.systemDefault, false)
                    .where(clearDefault)
                    .execute();
        }
        if (printTemplate.getCreatedAt() == null) {
            printTemplate.setCreatedAt(LocalDateTime.now());
        }
        if (printTemplate.getSystemDefault() == null) {
            printTemplate.setSystemDefault(false);
        }
        return printTemplateRepository.save(printTemplate);
    }

    @Transactional
    public void delete(Long printTemplateId, Long merchantId, Long accountBookId) {
        jqf.delete(qPrintTemplate)
                .where(qPrintTemplate.id.eq(printTemplateId)
                        .and(qPrintTemplate.merchantId.eq(merchantId))
                        .and(qPrintTemplate.accountBookId.eq(accountBookId)))
                .execute();
    }

    public PrintTemplate load(Long printTemplateId, Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPrintTemplate)
                .where(qPrintTemplate.id.eq(printTemplateId)
                        .and(qPrintTemplate.merchantId.eq(merchantId))
                        .and(qPrintTemplate.accountBookId.eq(accountBookId)))
                .fetchOne();
    }

    public List<PrintTemplate> byType(String documentType, Long merchantId, Long accountBookId) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPrintTemplate.merchantId.eq(merchantId))
                .and(qPrintTemplate.accountBookId.eq(accountBookId));
        if (StrUtil.isNotBlank(documentType)) {
            builder.and(qPrintTemplate.documentType.eq(PrintTemplate.DocumentType.valueOf(documentType)));
        }
        return bqf.selectFrom(qPrintTemplate)
                .where(builder)
                .orderBy(qPrintTemplate.systemDefault.desc(), qPrintTemplate.id.desc())
                .fetch();
    }

    public List<PrintTemplate> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPrintTemplate)
                .where(qPrintTemplate.merchantId.eq(merchantId).and(qPrintTemplate.accountBookId.eq(accountBookId)))
                .fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPrintTemplate.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPrintTemplate.accountBookId, accountBookId);
        }

        public void setName(String name) {
            if (StrUtil.isNotBlank(name)) {
                builder.and(qPrintTemplate.name.contains(name));
            }
        }

        public void setDocumentType(String documentType) {
            if (StrUtil.isNotBlank(documentType)) {
                builder.and(qPrintTemplate.documentType.eq(PrintTemplate.DocumentType.valueOf(documentType)));
            }
        }
    }
}
