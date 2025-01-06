package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.OtherIncome;
import com.flyemu.share.entity.fund.QOtherIncome;
import com.flyemu.share.repository.OtherIncomeRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 其他收入单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherIncomeService extends AbsService {

    private final static QOtherIncome qOtherIncome = QOtherIncome.otherIncome;

    private final OtherIncomeRepository otherIncomeRepository;

    public PageResults<OtherIncome> query(Page page, OtherIncomeService.Query query) {
        PagedList<OtherIncome> fetchPage = bqf.selectFrom(qOtherIncome).where(query.builder).orderBy(qOtherIncome.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherIncome> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherIncome otherIncome1 = tuple;
            OtherIncome otherIncome = BeanUtil.toBean(otherIncome1, OtherIncome.class);
            dtos.add(otherIncome);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherIncome save(OtherIncome otherIncome) {
        if (otherIncome.getId() != null) {
            //更新
            OtherIncome original = otherIncomeRepository.getById(otherIncome.getId());
            BeanUtil.copyProperties(otherIncome, original, CopyOptions.create().ignoreNullValue());
            return otherIncomeRepository.save(original);
        }
        return otherIncomeRepository.save(otherIncome);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherIncome)
                .where(qOtherIncome.id.eq(supplierFlowId).and(qOtherIncome.merchantId.eq(merchantId)).and(qOtherIncome.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherIncome> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherIncome).where(qOtherIncome.merchantId.eq(merchantId).and(qOtherIncome.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherIncome.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherIncome.accountBookId.eq(accountBookId));
            }
        }

    }
}
