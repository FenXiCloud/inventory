package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CodeRuleRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.EnumPath;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 编码规则
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CodeRuleService extends AbsService {

    private final static QCodeRule qCodeRule = QCodeRule.codeRule;

    private final CodeRuleRepository codeRuleRepository;
    /**
     * 根据单据类型、商户ID和账本ID查询系统默认的编码规则（只返回一条）
     */
    public CodeRule findByDocumentTypeAndMerchantIdAndAccountBookId(CodeRule.DocumentType documentType, Long merchantId, Long accountBookId) {

        CodeRule codeRule = bqf.selectFrom(qCodeRule)
                .where(qCodeRule.documentType.eq(documentType)
                        .and(qCodeRule.merchantId.eq(merchantId))
                        .and(qCodeRule.accountBookId.eq(accountBookId))
                        .and(qCodeRule.systemDefault.eq(true)))
                .fetchFirst();

        return codeRule;
    }
    public List<CodeRule> query(Query query) {
        List<CodeRule> codeRules = bqf.selectFrom(qCodeRule)
                .where(query.builder)
                .orderBy(qCodeRule.id.desc())
                .fetch();
        return codeRules;
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
            //更新
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

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qCodeRule.name.contains(name));
            }
        }
        public void setDocumentType(String documentType) {
            if (StrUtil.isNotEmpty(documentType)) {
                EnumPath<CodeRule.DocumentType> documentTypeEnumPath = qCodeRule.documentType;
                builder.and(documentTypeEnumPath.eq(CodeRule.DocumentType.valueOf(documentType)));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCodeRule.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCodeRule.accountBookId.eq(accountBookId));
            }
        }

        public void setSystemDefault(Boolean systemDefault) {
            if (systemDefault != null) {
                builder.and(qCodeRule.systemDefault.eq(systemDefault));
            }
        }

    }
}
