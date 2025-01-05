package com.flyemu.share.service.sales;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.repository.SalesOutboundRepository;
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
public class SalesOutboundService extends AbsService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;

    private final SalesOutboundRepository salesOutboundRepository;

    public PageResults<SalesOutbound> query(Page page, SalesOutboundService.Query query) {
        PagedList<SalesOutbound> fetchPage = bqf.selectFrom(qSalesOutbound).where(query.builder).orderBy(qSalesOutbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<SalesOutbound> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOutbound salesOutbound1 = tuple;
            SalesOutbound salesOutbound = BeanUtil.toBean(salesOutbound1, SalesOutbound.class);
            dtos.add(salesOutbound);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public SalesOutbound save(SalesOutbound salesOutbound) {
        if (salesOutbound.getId() != null) {
            //更新
            SalesOutbound original = salesOutboundRepository.getById(salesOutbound.getId());
            BeanUtil.copyProperties(salesOutbound, original, CopyOptions.create().ignoreNullValue());
            return salesOutboundRepository.save(original);
        }
        return salesOutboundRepository.save(salesOutbound);
    }

    @Transactional
    public void delete(Long salesOutboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qSalesOutbound)
                .where(qSalesOutbound.id.eq(salesOutboundId).and(qSalesOutbound.merchantId.eq(merchantId)).and(qSalesOutbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesOutbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesOutbound).where(qSalesOutbound.merchantId.eq(merchantId).and(qSalesOutbound.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOutbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOutbound.accountBookId.eq(accountBookId));
            }
        }
    }
}
