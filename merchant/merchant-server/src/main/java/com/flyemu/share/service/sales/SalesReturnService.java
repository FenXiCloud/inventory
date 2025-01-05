package com.flyemu.share.service.sales;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.sales.QSalesReturn;
import com.flyemu.share.entity.sales.SalesReturn;
import com.flyemu.share.repository.SalesReturnRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 销售出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReturnService extends AbsService {

    private final static QSalesReturn qSalesReturn = QSalesReturn.salesReturn;

    private final SalesReturnRepository salesReturnRepository;

    public PageResults<SalesReturn> query(Page page, SalesReturnService.Query query) {
        PagedList<SalesReturn> fetchPage = bqf.selectFrom(qSalesReturn).where(query.builder).orderBy(qSalesReturn.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<SalesReturn> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesReturn salesReturn1 = tuple;
            SalesReturn salesReturn = BeanUtil.toBean(salesReturn1, SalesReturn.class);
            dtos.add(salesReturn);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public SalesReturn save(SalesReturn salesReturn) {
        if (salesReturn.getId() != null) {
            //更新
            SalesReturn original = salesReturnRepository.getById(salesReturn.getId());
            BeanUtil.copyProperties(salesReturn, original, CopyOptions.create().ignoreNullValue());
            return salesReturnRepository.save(original);
        }
        return salesReturnRepository.save(salesReturn);
    }

    @Transactional
    public void delete(Long SalesReturnId, Long merchantId, Long accountBookId) {
        jqf.delete(qSalesReturn)
                .where(qSalesReturn.id.eq(SalesReturnId).and(qSalesReturn.merchantId.eq(merchantId)).and(qSalesReturn.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesReturn> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesReturn).where(qSalesReturn.merchantId.eq(merchantId).and(qSalesReturn.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesReturn.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesReturn.accountBookId.eq(accountBookId));
            }
        }
    }
}
