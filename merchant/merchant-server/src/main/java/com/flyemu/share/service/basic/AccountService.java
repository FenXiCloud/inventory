package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.Account;
import com.flyemu.share.entity.basic.QAccount;
import com.flyemu.share.repository.AccountRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 账户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountService extends AbsService {

    private final static QAccount qAccount = QAccount.account;

    private final AccountRepository accountRepository;

    public List<Account> query(Query query) {
        List<Account> accounts = bqf.selectFrom(qAccount)
                .where(query.builder)
                .orderBy(qAccount.id.desc())
                .fetch();
        return accounts;
    }

    @Transactional
    public Account save(Account account) {
        if (account.getId() != null) {
            //更新
            Account original = accountRepository.getById(account.getId());
            BeanUtil.copyProperties(account, original, CopyOptions.create().ignoreNullValue());
            return accountRepository.save(original);
        }
        return accountRepository.save(account);
    }

    @Transactional
    public void delete(Long accountId, Long merchantId, Long accountBookId) {
        jqf.delete(qAccount)
                .where(qAccount.id.eq(accountId).and(qAccount.merchantId.eq(merchantId)).and(qAccount.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Account> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qAccount).where(qAccount.merchantId.eq(merchantId).and(qAccount.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAccount.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qAccount.accountBookId.eq(accountBookId));
            }
        }
    }
}
