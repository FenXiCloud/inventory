package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.dto.price.PricingPolicyDto;
import com.flyemu.share.entity.basic.PricingPolicy;
import com.flyemu.share.entity.basic.QPricingPolicy;
import com.flyemu.share.enums.PolicyType;
import com.flyemu.share.form.price.PricingPolicyForm;
import com.flyemu.share.repository.basic.PricingPolicyRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PricingPolicyService extends BaseService {

    private final static QPricingPolicy qPricingPolicy = QPricingPolicy.pricingPolicy;

    private final PricingPolicyRepository pricingPolicyRepository;
    private final PriceResolveService priceResolveService;

    public List<PricingPolicy> query(Query query) {
        if (query.merchantId != null && query.accountBookId != null) {
            priceResolveService.ensureDefaultPolicies(query.merchantId, query.accountBookId);
        }
        return bqf.selectFrom(qPricingPolicy)
                .where(query.builder)
                .orderBy(qPricingPolicy.priority.asc())
                .fetch();
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
        priceResolveService.ensureDefaultPolicies(merchantId, accountBookId);
        return bqf.selectFrom(qPricingPolicy).where(qPricingPolicy.merchantId.eq(merchantId).and(qPricingPolicy.accountBookId.eq(accountBookId))).fetch();
    }
    //重新排好顺序
    @Transactional
    public void sort(PricingPolicyForm pricingPolicyForm, Long merchantId, Long accountBookId) {
        List<PricingPolicyDto> dataList = pricingPolicyForm.getDataList();
        if (dataList == null || dataList.isEmpty()) {
            return;
        }
        for (int i = 0; i < dataList.size(); i++) {
            PricingPolicyDto pricingPolicyDTO = dataList.get(i);
            PricingPolicy pricingPolicy = bqf.selectFrom(qPricingPolicy)
                    .where(qPricingPolicy.id.eq(pricingPolicyDTO.getId())
                            .and(qPricingPolicy.merchantId.eq(merchantId))
                            .and(qPricingPolicy.accountBookId.eq(accountBookId)))
                    .fetchOne();
            if (pricingPolicy == null) {
                continue;
            }
            pricingPolicy.setPriority(i + 1);
            pricingPolicy.setEnabled(pricingPolicyDTO.getEnabled());
            pricingPolicyRepository.save(pricingPolicy);
        }
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();
        private Long merchantId;
        private Long accountBookId;

        public void setPolicyType(PolicyType policyType) {
            if (policyType != null) {
                builder.and(qPricingPolicy.policyType.eq(policyType));
            } else {
                builder.and(qPricingPolicy.policyType.isNotNull());
            }
        }

        public void setMerchantId(Long merchantId) {
            this.merchantId = merchantId;
            TenantFilters.merchant(builder, qPricingPolicy.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            this.accountBookId = accountBookId;
            TenantFilters.accountBook(builder, qPricingPolicy.accountBookId, accountBookId);
        }
    }
}
