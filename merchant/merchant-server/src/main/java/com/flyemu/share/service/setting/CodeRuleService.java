package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QCodeRule;
import com.flyemu.share.repository.CodeRuleRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
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

    public List<CodeRule> query(Query query) {
        List<CodeRule> codeRules = bqf.selectFrom(qCodeRule)
                .where(query.builder)
                .orderBy(qCodeRule.id.desc())
                .fetch();
        return codeRules;
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
        jqf.delete(qCodeRule)
                .where(qCodeRule.id.eq(codeRuleId).and(qCodeRule.merchantId.eq(merchantId)).and(qCodeRule.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<CodeRule> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCodeRule).where(qCodeRule.merchantId.eq(merchantId).and(qCodeRule.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

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
    }
}
