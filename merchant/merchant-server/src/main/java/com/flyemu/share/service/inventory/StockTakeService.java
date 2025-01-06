package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.QStockTake;
import com.flyemu.share.entity.inventory.StockTake;
import com.flyemu.share.repository.StockTakeRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 盘点单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockTakeService extends AbsService {

    private final static QStockTake qStockTake = QStockTake.stockTake;

    private final StockTakeRepository stockTakeRepository;

    public PageResults<StockTake> query(Page page, Query query) {
        PagedList<StockTake> fetchPage = bqf.selectFrom(qStockTake).where(query.builder).orderBy(qStockTake.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<StockTake> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            StockTake stockTake1 = tuple;
            StockTake stockTake = BeanUtil.toBean(stockTake1, StockTake.class);
            dtos.add(stockTake);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public StockTake save(StockTake stockTake) {
        if (stockTake.getId() != null) {
            //更新
            StockTake original = stockTakeRepository.getById(stockTake.getId());
            BeanUtil.copyProperties(stockTake, original, CopyOptions.create().ignoreNullValue());
            return stockTakeRepository.save(original);
        }
        return stockTakeRepository.save(stockTake);
    }

    @Transactional
    public void delete(Long stockTakeId, Long merchantId, Long accountBookId) {
        jqf.delete(qStockTake)
                .where(qStockTake.id.eq(stockTakeId).and(qStockTake.merchantId.eq(merchantId)).and(qStockTake.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<StockTake> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qStockTake).where(qStockTake.merchantId.eq(merchantId).and(qStockTake.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qStockTake.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qStockTake.accountBookId.eq(accountBookId));
            }
        }
    }
}
