package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.AccountTransfer;
import com.flyemu.share.entity.fund.QAccountTransfer;
import com.flyemu.share.repository.AccountTransferRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 资金转账
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountTransferService extends AbsService {

    private final static QAccountTransfer qAccountTransfer = QAccountTransfer.accountTransfer;

    private final AccountTransferRepository accountTransferRepository;

    public PageResults<AccountTransfer> query(Page page, AccountTransferService.Query query) {
        PagedList<AccountTransfer> fetchPage = bqf.selectFrom(qAccountTransfer).where(query.builder).orderBy(qAccountTransfer.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<AccountTransfer> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AccountTransfer accountTransfer1 = tuple;
            AccountTransfer accountTransfer = BeanUtil.toBean(accountTransfer1, AccountTransfer.class);
            dtos.add(accountTransfer);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public AccountTransfer save(AccountTransfer accountTransfer) {
        if (accountTransfer.getId() != null) {
            //更新
            AccountTransfer original = accountTransferRepository.getById(accountTransfer.getId());
            BeanUtil.copyProperties(accountTransfer, original, CopyOptions.create().ignoreNullValue());
            return accountTransferRepository.save(original);
        }
        return accountTransferRepository.save(accountTransfer);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qAccountTransfer)
                .where(qAccountTransfer.id.eq(supplierFlowId).and(qAccountTransfer.merchantId.eq(merchantId)).and(qAccountTransfer.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<AccountTransfer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qAccountTransfer).where(qAccountTransfer.merchantId.eq(merchantId).and(qAccountTransfer.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAccountTransfer.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qAccountTransfer.accountBookId.eq(accountBookId));
            }
        }

    }
}
