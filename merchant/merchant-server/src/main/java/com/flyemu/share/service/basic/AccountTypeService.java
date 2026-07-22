package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.AccountType;
import com.flyemu.share.entity.basic.QAccountType;
import com.flyemu.share.repository.basic.AccountTypeRepository;
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
public class AccountTypeService extends BaseService {

    private final static QAccountType qAccountType = QAccountType.accountType;

    private final AccountTypeRepository accountTypeRepository;

    public List<AccountType> query(Query query) {
        return bqf.selectFrom(qAccountType)
                .where(query.builder)
                .orderBy(qAccountType.id.desc())
                .fetch();
    }

    @Transactional
    public AccountType save(AccountType accountType) {
        if (accountType.getId() != null) {
            //更新
            AccountType original = accountTypeRepository.getById(accountType.getId());
            BeanUtil.copyProperties(accountType, original, CopyOptions.create().ignoreNullValue());
            return accountTypeRepository.save(original);
        }
        return accountTypeRepository.save(accountType);
    }

    @Transactional
    public void delete(Long accountTypeId, Long merchantId, Long accountBookId) {
        jqf.delete(qAccountType)
                .where(qAccountType.id.eq(accountTypeId).and(qAccountType.merchantId.eq(merchantId)).and(qAccountType.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<AccountType> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qAccountType).where(qAccountType.merchantId.eq(merchantId).and(qAccountType.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String  name) {
            if (name != null&&name!="") {
                builder.and(qAccountType.name.like("%" + name + "%"));
            }
        }

        public void setCostType(AccountType.CostType costType) {
            if (costType != null) {
                builder.and(qAccountType.costType.eq(costType));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qAccountType.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qAccountType.accountBookId, accountBookId);
        }
    }
}
