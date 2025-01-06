package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.OtherExpense;
import com.flyemu.share.entity.fund.QOtherExpense;
import com.flyemu.share.repository.OtherExpenseRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 其他支付单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherExpenseService extends AbsService {

    private final static QOtherExpense qOtherExpense = QOtherExpense.otherExpense;

    private final OtherExpenseRepository otherExpenseRepository;

    public PageResults<OtherExpense> query(Page page, OtherExpenseService.Query query) {
        PagedList<OtherExpense> fetchPage = bqf.selectFrom(qOtherExpense).where(query.builder).orderBy(qOtherExpense.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherExpense> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherExpense otherExpense1 = tuple;
            OtherExpense otherExpense = BeanUtil.toBean(otherExpense1, OtherExpense.class);
            dtos.add(otherExpense);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherExpense save(OtherExpense otherExpense) {
        if (otherExpense.getId() != null) {
            //更新
            OtherExpense original = otherExpenseRepository.getById(otherExpense.getId());
            BeanUtil.copyProperties(otherExpense, original, CopyOptions.create().ignoreNullValue());
            return otherExpenseRepository.save(original);
        }
        return otherExpenseRepository.save(otherExpense);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherExpense)
                .where(qOtherExpense.id.eq(supplierFlowId).and(qOtherExpense.merchantId.eq(merchantId)).and(qOtherExpense.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherExpense> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherExpense).where(qOtherExpense.merchantId.eq(merchantId).and(qOtherExpense.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherExpense.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherExpense.accountBookId.eq(accountBookId));
            }
        }

    }
}
