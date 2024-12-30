package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.entity.setting.QAccountBook;
import com.flyemu.share.entity.setting.QMerchant;
import com.flyemu.share.repository.AccountBookRepository;
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
public class AccountBookService extends AbsService {

    private static final QAccountBook qAccountBook = QAccountBook.accountBook;

    private static final QMerchant qMerchant = QMerchant.merchant;

    private final AccountBookRepository accountBookRepository;

    public PageResults<AccountBook> query(Page page, Query query) {
        PagedList<AccountBook> fetchPage = bqf.selectFrom(qAccountBook)
                .where(query.builder)
                .orderBy(qAccountBook.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page);
    }

    @Transactional
    public AccountBook save(AccountBook accountBook) {
        if (accountBook.getId() != null) {
            //更新
            AccountBook original = accountBookRepository.getById(accountBook.getId());
            BeanUtil.copyProperties(accountBook, original, CopyOptions.create().ignoreNullValue());
            return accountBookRepository.save(original);
        }
        accountBook.setEnabled(true);
        return accountBookRepository.save(accountBook);
    }

    @Transactional
    public void delete(Long accountBookId) {
        accountBookRepository.deleteById(accountBookId);
    }

    public List<AccountBook> listAll(Long merchantId) {
        return bqf.selectFrom(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.enabled.isTrue()))
                .orderBy(qAccountBook.id.asc())
                .fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qAccountBook.name.contains(name));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAccountBook.merchantId.eq(merchantId));
            }
        }
    }
}
