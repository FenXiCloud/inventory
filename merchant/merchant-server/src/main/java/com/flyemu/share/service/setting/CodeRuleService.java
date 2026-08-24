package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.CodeRuleRepository;
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
public class CodeRuleService extends BaseService {

    private final static QCodeRule qCodeRule = QCodeRule.codeRule;

    private final CodeRuleRepository codeRuleRepository;

    private static final List<CodeRule.DocumentType> DEFAULT_DOCUMENT_TYPES = List.of(
            CodeRule.DocumentType.采购订单,
            CodeRule.DocumentType.采购入库单,
            CodeRule.DocumentType.采购退货单,
            CodeRule.DocumentType.销售订单,
            CodeRule.DocumentType.销售出库单,
            CodeRule.DocumentType.销售退货单,
            CodeRule.DocumentType.调拨单,
            CodeRule.DocumentType.盘点单,
            CodeRule.DocumentType.其他入库单,
            CodeRule.DocumentType.其他出库单,
            CodeRule.DocumentType.成本调整单,
            CodeRule.DocumentType.收款单,
            CodeRule.DocumentType.付款单,
            CodeRule.DocumentType.核销单,
            CodeRule.DocumentType.结算单,
            CodeRule.DocumentType.其他收款单,
            CodeRule.DocumentType.其他付款单,
            CodeRule.DocumentType.转帐单,
            CodeRule.DocumentType.商品,
            CodeRule.DocumentType.仓库,
            CodeRule.DocumentType.客户,
            CodeRule.DocumentType.供货商,
            CodeRule.DocumentType.组装拆卸单,
            CodeRule.DocumentType.销售预订,
            CodeRule.DocumentType.进货预订,
            CodeRule.DocumentType.拣货单
    );

    /**
     * 根据单据类型、商户ID和账本ID查询系统默认的编码规则（只返回一条）
     */
    public CodeRule findByDocumentTypeAndMerchantIdAndAccountBookId(CodeRule.DocumentType documentType, Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCodeRule)
                .where(qCodeRule.documentType.eq(documentType)
                        .and(qCodeRule.merchantId.eq(merchantId))
                        .and(qCodeRule.accountBookId.eq(accountBookId))
                        .and(qCodeRule.systemDefault.eq(true)))
                .fetchFirst();
    }

    /**
     * 确保账套下存在默认编码规则（缺失时补齐）
     */
    @Transactional
    public void ensureDefaultRules(Long merchantId, Long accountBookId) {
        for (CodeRule.DocumentType type : DEFAULT_DOCUMENT_TYPES) {
            Long count = jqf.select(qCodeRule.id.count())
                    .from(qCodeRule)
                    .where(qCodeRule.merchantId.eq(merchantId)
                            .and(qCodeRule.accountBookId.eq(accountBookId))
                            .and(qCodeRule.documentType.eq(type)))
                    .fetchOne();
            if (count != null && count > 0) {
                continue;
            }
            CodeRule rule = new CodeRule();
            rule.setName("初始化");
            rule.setDocumentType(type);
            rule.setPrefix(getDefaultPrefix(type));
            rule.setFormat("yyyyMMdd");
            rule.setSerialNumberLength(5);
            rule.setStartValue(1);
            rule.setResetPeriod(CodeRule.ResetPeriod.日);
            rule.setSystemDefault(true);
            rule.setMerchantId(merchantId);
            rule.setAccountBookId(accountBookId);
            rule.setCreatedAt(LocalDateTime.now());
            codeRuleRepository.save(rule);
        }
    }

    private String getDefaultPrefix(CodeRule.DocumentType type) {
        return switch (type) {
            case 采购订单 -> "PO";
            case 采购入库单 -> "PI";
            case 采购退货单 -> "PR";
            case 销售订单 -> "SO";
            case 销售出库单 -> "DO";
            case 销售退货单 -> "SR";
            case 调拨单 -> "TR";
            case 盘点单 -> "IC";
            case 其他入库单 -> "OI";
            case 其他出库单 -> "OO";
            case 成本调整单 -> "CA";
            case 收款单 -> "RC";
            case 付款单 -> "PY";
            case 核销单 -> "RV";
            case 结算单 -> "ST";
            case 其他收款单 -> "OR";
            case 其他付款单 -> "OP";
            case 转帐单 -> "TF";
            case 商品 -> "PD";
            case 仓库 -> "WH";
            case 客户 -> "CU";
            case 供货商 -> "SU";
            case 组装拆卸单 -> "AS";
            case 销售预订 -> "XS";
            case 进货预订 -> "JH";
            case 拣货单 -> "PK";
        };
    }

    public List<CodeRule> query(Query query) {
        return bqf.selectFrom(qCodeRule)
                .where(query.builder)
                .orderBy(qCodeRule.id.desc())
                .fetch();
    }

    public List<CodeRule> queryEnable(CodeRule codeRule, Long accountBookId, Long merchantId) {
        return bqf.selectFrom(qCodeRule).where(qCodeRule.merchantId.eq(merchantId)
                .and(qCodeRule.accountBookId.eq(accountBookId))
                .and(qCodeRule.documentType.eq(codeRule.getDocumentType()))
                .and(qCodeRule.systemDefault.eq(codeRule.getSystemDefault()))
                .and(qCodeRule.id.ne(codeRule.getId()))
        ).fetch();
    }

    @Transactional
    public CodeRule save(CodeRule codeRule) {
        if (codeRule.getId() != null) {
            if (Boolean.TRUE.equals(codeRule.getSystemDefault())) {
                List<CodeRule> codeRuleList = queryEnable(codeRule, codeRule.getAccountBookId(), codeRule.getMerchantId());
                if (!codeRuleList.isEmpty()) {
                    throw new ServiceException("每种单据类型只能存在一个默认");
                }
            }
            CodeRule original = codeRuleRepository.getById(codeRule.getId());
            BeanUtil.copyProperties(codeRule, original, CopyOptions.create().ignoreNullValue());
            return codeRuleRepository.save(original);
        }
        return codeRuleRepository.save(codeRule);
    }

    @Transactional
    public void delete(Long codeRuleId, Long merchantId, Long accountBookId) {

        CodeRule toDelete = codeRuleRepository.findById(codeRuleId)
                .orElseThrow(() -> new ServiceException("编码规则不存在"));

        Long count = jqf.select(qCodeRule.id.count())
                .from(qCodeRule)
                .where(qCodeRule.merchantId.eq(merchantId)
                        .and(qCodeRule.accountBookId.eq(accountBookId))
                        .and(qCodeRule.documentType.eq(toDelete.getDocumentType())))
                .fetchOne();

        if (count != null && count <= 1) {
            throw new ServiceException("该单据类型下必须至少保留一个编码规则，无法删除");
        }

        long deleted = jqf.delete(qCodeRule)
                .where(qCodeRule.id.eq(codeRuleId)
                        .and(qCodeRule.merchantId.eq(merchantId))
                        .and(qCodeRule.accountBookId.eq(accountBookId)))
                .execute();

        if (deleted == 0) {
            throw new ServiceException("删除失败");
        }
    }

    public List<CodeRule> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCodeRule).where(qCodeRule.merchantId.eq(merchantId).and(qCodeRule.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qCodeRule.name.contains(name));
            }
        }

        public void setDocumentType(String documentType) {
            if (StrUtil.isNotEmpty(documentType)) {
                // 前端历史文案「产品」与枚举「商品」对齐
                if ("产品".equals(documentType)) {
                    documentType = "商品";
                }
                try {
                    builder.and(qCodeRule.documentType.eq(CodeRule.DocumentType.valueOf(documentType)));
                } catch (IllegalArgumentException e) {
                    throw new ServiceException("不支持的单据类型：" + documentType);
                }
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCodeRule.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCodeRule.accountBookId, accountBookId);
        }

        public void setSystemDefault(Boolean systemDefault) {
            if (systemDefault != null) {
                builder.and(qCodeRule.systemDefault.eq(systemDefault));
            }
        }

    }
}
