package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.entity.setting.PricingPolicy;
import com.flyemu.share.entity.setting.QPricingPolicy;
import com.flyemu.share.repository.PaymentMethodRepository;
import com.flyemu.share.repository.PricingPolicyRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 价格取数规则
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PricingPolicyService extends AbsService {

    private final static QPricingPolicy qPricingPolicy = QPricingPolicy.pricingPolicy;

    private final PricingPolicyRepository pricingPolicyRepository;

    public List<PricingPolicy> query(Query query) {
        List<PricingPolicy> pricingPolicys = bqf.selectFrom(qPricingPolicy)
                .where(query.builder)
                .orderBy(qPricingPolicy.id.desc())
                .fetch();
        return pricingPolicys;
    }

    @Transactional
    public PricingPolicy save(PricingPolicy pricingPolicy) {
        if (pricingPolicy.getId() != null) {
            //更新
            PricingPolicy original = pricingPolicyRepository.getById(pricingPolicy.getId());
            BeanUtil.copyProperties(pricingPolicy, original, CopyOptions.create().ignoreNullValue());
            return pricingPolicyRepository.save(original);
        }
        return pricingPolicyRepository.save(pricingPolicy);
    }

    @Transactional
    public void delete(Long pricingPolicyId, Long merchantId, Long accountBookId) {
        jqf.delete(qPricingPolicy)
                .where(qPricingPolicy.id.eq(pricingPolicyId).and(qPricingPolicy.merchantId.eq(merchantId)).and(qPricingPolicy.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PricingPolicy> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPricingPolicy).where(qPricingPolicy.merchantId.eq(merchantId).and(qPricingPolicy.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPricingPolicy.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPricingPolicy.accountBookId.eq(accountBookId));
            }
        }
    }
}
