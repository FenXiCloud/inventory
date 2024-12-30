package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AccountBookDto;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.entity.setting.QAccountBook;
import com.flyemu.share.entity.setting.QCheckout;
import com.flyemu.share.entity.setting.QMerchant;
import com.flyemu.share.repository.*;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountBookService extends AbsService {

    private static final QAccountBook qAccountBook = QAccountBook.accountBook;

    private static final QCheckout qCheckout = QCheckout.checkout;

    private final AccountBookRepository accountBookRepository;

    private final QMerchant qMerchant = QMerchant.merchant;

    public PageResults<AccountBookDto> query(Page page, Query query) {
        PagedList<AccountBook> fetchPage = bqf.selectFrom(qAccountBook).where(query.builder).orderBy(qAccountBook.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<AccountBookDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AccountBook accountBook = tuple;
            AccountBookDto accountBookDto = BeanUtil.toBean(accountBook, AccountBookDto.class);
            dtos.add(accountBookDto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }


    /**
     * loadAccountBooks 加载账套列表
     *
     * @param merchantId
     * @return
     */
    public List<Dict> loadAccountBooks(Long merchantId) {
        List<Dict> dictList = new ArrayList<>();
        bqf.selectFrom(qAccountBook).select(qAccountBook.id, qAccountBook.name, qAccountBook.current, qAccountBook.startDate, qAccountBook.checkoutDate).where(qAccountBook.merchantId.eq(merchantId))
                .orderBy(qAccountBook.id.desc()).fetch().forEach(tuple -> {
                    Dict dict = new Dict().set("key", tuple.get(qAccountBook.id))
                            .set("title", tuple.get(qAccountBook.name))
                            .set("startDate", tuple.get(qAccountBook.startDate))
                            .set("checkoutDate", tuple.get(qAccountBook.checkoutDate))
                            .set("current", tuple.get(qAccountBook.current));
                    dictList.add(dict);
                });
        return dictList;
    }

    @Transactional
    public AccountBook changeCurrentAccountBook(Long merchantId, Long orgId) {
        jqf.update(qAccountBook)
                .set(qAccountBook.current, false)
                .where(qAccountBook.merchantId.eq(merchantId)).execute();
        jqf.update(qAccountBook)
                .set(qAccountBook.current, true)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(orgId))).execute();

        return bqf.selectFrom(qAccountBook).where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(orgId))).fetchFirst();
    }


    /**
     * 保存/更新
     *
     * @param accountBookDto
     * @return
     */
    @Transactional
    public AccountBook save(AccountBookDto accountBookDto) {
        if (bqf.selectFrom(qAccountBook)
                .where(qAccountBook.merchantId.eq(accountBookDto.getMerchantId())).fetchCount() <= 0) {
            accountBookDto.setCurrent(true);
        }
        if (accountBookDto.getId() != null) {
            //更新
            AccountBook original = accountBookRepository.getById(accountBookDto.getId());
            BeanUtil.copyProperties(accountBookDto, original, CopyOptions.create().ignoreNullValue());
            return accountBookRepository.save(original);
        }

        AccountBook accountBook = BeanUtil.toBean(accountBookDto, AccountBook.class);
        accountBookRepository.save(accountBook);
        return accountBook;
    }

    /**
     * 删除
     *
     * @param merchantId
     * @param accountBookId
     */
    @Transactional
    public void delete(Long merchantId, Long accountBookId) {

        jqf.delete(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .execute();
    }

    /**
     * 根据ID获取数据
     *
     * @param accountBookId
     * @return
     */
    public AccountBook loadById(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .fetchFirst();
    }

    public List<AccountBook> select(Long merchantId) {
        return bqf.selectFrom(qAccountBook).where(qAccountBook.merchantId.eq(merchantId)).fetch();
    }


    /**
     * 查询条件
     */
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
