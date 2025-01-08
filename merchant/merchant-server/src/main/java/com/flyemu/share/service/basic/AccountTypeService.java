package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.AccountType;
import com.flyemu.share.entity.basic.QAccountType;
import com.flyemu.share.repository.AccountTypeRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 账户收支类别
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountTypeService extends AbsService {

    private final static QAccountType qAccountType = QAccountType.accountType;

    private final AccountTypeRepository accountTypeRepository;

    public List<AccountType> query(Query query) {
        List<AccountType> accountTypes = bqf.selectFrom(qAccountType)
                .where(query.builder)
                .orderBy(qAccountType.id.desc())
                .fetch();
        return accountTypes;
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

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();
        public void setName(String  name) {
            if (name != null&&name!="") {
                builder.and(qAccountType.name.like("%" + name + "%"));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAccountType.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qAccountType.accountBookId.eq(accountBookId));
            }
        }
    }
}
