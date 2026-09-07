package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.setting.PrintTemplate;
import com.flyemu.share.entity.setting.QPrintTemplate;
import com.flyemu.share.exception.ServiceException;
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
            boolean wasDefault = Boolean.TRUE.equals(original.getSystemDefault());
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
            } else if (wasDefault && original.getDocumentType() != null) {
                // 正在把“默认模板”取消：每个单据类型必须至少保留一个默认模板
                Long otherDefault = jqf.select(qPrintTemplate.id.count())
                        .from(qPrintTemplate)
                        .where(qPrintTemplate.merchantId.eq(original.getMerchantId())
                                .and(qPrintTemplate.accountBookId.eq(original.getAccountBookId()))
                                .and(qPrintTemplate.documentType.eq(original.getDocumentType()))
                                .and(qPrintTemplate.systemDefault.isTrue())
                                .and(qPrintTemplate.id.ne(original.getId())))
                        .fetchOne();
                if (otherDefault == null || otherDefault == 0) {
                    throw new ServiceException("每个单据类型必须保留一个默认模板，请先将其他模板设为默认，再取消当前默认模板");
                }
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
        // 保底：若该单据类型当前连一个默认模板都没有（旧数据/恢复后可能出现），新增的非默认模板自动置为默认
        if (printTemplate.getDocumentType() != null
                && !Boolean.TRUE.equals(printTemplate.getSystemDefault())
                && printTemplate.getMerchantId() != null
                && printTemplate.getAccountBookId() != null) {
            Long defaultCount = jqf.select(qPrintTemplate.id.count())
                    .from(qPrintTemplate)
                    .where(qPrintTemplate.merchantId.eq(printTemplate.getMerchantId())
                            .and(qPrintTemplate.accountBookId.eq(printTemplate.getAccountBookId()))
                            .and(qPrintTemplate.documentType.eq(printTemplate.getDocumentType()))
                            .and(qPrintTemplate.systemDefault.isTrue()))
                    .fetchOne();
            if (defaultCount == null || defaultCount == 0) {
                printTemplate.setSystemDefault(Boolean.TRUE);
            }
        }
        return printTemplateRepository.save(printTemplate);
    }

    @Transactional
    public void delete(Long printTemplateId, Long merchantId, Long accountBookId) {
        PrintTemplate template = bqf.selectFrom(qPrintTemplate)
                .where(qPrintTemplate.id.eq(printTemplateId)
                        .and(qPrintTemplate.merchantId.eq(merchantId))
                        .and(qPrintTemplate.accountBookId.eq(accountBookId)))
                .fetchOne();
        if (template == null) {
            return;
        }
        // 不允许删除默认模板
        if (Boolean.TRUE.equals(template.getSystemDefault())) {
            throw new ServiceException("默认模板不允许删除，可先将其他模板设为默认后再删除");
        }
        // 每个单据类型至少保留一个打印模板
        Long count = jqf.select(qPrintTemplate.id.count())
                .from(qPrintTemplate)
                .where(qPrintTemplate.merchantId.eq(merchantId)
                        .and(qPrintTemplate.accountBookId.eq(accountBookId))
                        .and(qPrintTemplate.documentType.eq(template.getDocumentType())))
                .fetchOne();
        if (count == null || count <= 1) {
            throw new ServiceException("每个单据类型必须至少保留一个打印模板，不能删除最后一个模板");
        }
        jqf.delete(qPrintTemplate)
                .where(qPrintTemplate.id.eq(printTemplateId)
                        .and(qPrintTemplate.merchantId.eq(merchantId))
                        .and(qPrintTemplate.accountBookId.eq(accountBookId)))
                .execute();
    }

    /**
     * 首次访问某单据类型时，若无任何打印模板则自动预置一个默认模板（系统预设，初次使用即默认）。
     */
    @Transactional
    public void ensureDefaultIfMissing(String documentType, Long merchantId, Long accountBookId) {
        if (StrUtil.isNotBlank(documentType)) {
            try {
                ensureDefaultIfMissing(PrintTemplate.DocumentType.valueOf(documentType), merchantId, accountBookId);
            } catch (IllegalArgumentException e) {
                log.warn("预置打印默认模板：忽略未知单据类型 {}", documentType);
            }
        }
    }

    @Transactional
    public void ensureDefaultIfMissing(PrintTemplate.DocumentType type, Long merchantId, Long accountBookId) {
        if (type == null || merchantId == null || accountBookId == null) {
            return;
        }
        Long count = jqf.select(qPrintTemplate.id.count())
                .from(qPrintTemplate)
                .where(qPrintTemplate.merchantId.eq(merchantId)
                        .and(qPrintTemplate.accountBookId.eq(accountBookId))
                        .and(qPrintTemplate.documentType.eq(type)))
                .fetchOne();
        if (count != null && count > 0) {
            return;
        }
        PrintTemplate preset = new PrintTemplate();
        preset.setName(type.name() + "（默认模板）");
        preset.setDocumentType(type);
        preset.setSystemDefault(Boolean.TRUE);
        preset.setMerchantId(merchantId);
        preset.setAccountBookId(accountBookId);
        preset.setCreatedAt(LocalDateTime.now());
        preset.setContent(buildDefaultContent());
        printTemplateRepository.save(preset);
        log.info("预置打印默认模板：type={}, merchantId={}, accountBookId={}", type, merchantId, accountBookId);
    }

    @Transactional
    public void ensureDefaultsForAll(Long merchantId, Long accountBookId) {
        for (PrintTemplate.DocumentType type : PrintTemplate.DocumentType.values()) {
            ensureDefaultIfMissing(type, merchantId, accountBookId);
        }
    }

    /**
     * 与前端默认模板一致的可打印字段：表头单据编号/单据日期/往来单位/金额/备注，明细产品名称/数量/单价/金额/备注。
     */
    private JSONArray buildDefaultContent() {
        JSONArray content = new JSONArray();
        addContentField(content, "header", "orderNo", "单据编号");
        addContentField(content, "header", "orderDate", "单据日期");
        addContentField(content, "header", "partner", "往来单位");
        addContentField(content, "header", "amount", "金额");
        addContentField(content, "header", "remarks", "备注");
        addContentField(content, "item", "productName", "产品名称");
        addContentField(content, "item", "quantity", "数量");
        addContentField(content, "item", "price", "单价");
        addContentField(content, "item", "amount", "金额");
        addContentField(content, "item", "remarks", "备注");
        return content;
    }

    private void addContentField(JSONArray content, String section, String key, String label) {
        JSONObject field = new JSONObject();
        field.put("key", key);
        field.put("label", label);
        field.put("section", section);
        field.put("enabled", Boolean.TRUE);
        content.add(field);
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
