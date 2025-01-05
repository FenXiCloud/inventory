package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.repository.PurchaseInboundRepository;
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
public class PurchaseInboundService extends AbsService {

    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;

    private final PurchaseInboundRepository purchaseInboundRepository;

    public PageResults<PurchaseInbound> query(Page page, Query query) {
        PagedList<PurchaseInbound> fetchPage = bqf.selectFrom(qPurchaseInbound).where(query.builder).orderBy(qPurchaseInbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseInbound> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseInbound purchaseInbound1 = tuple;
            PurchaseInbound purchaseInbound = BeanUtil.toBean(purchaseInbound1, PurchaseInbound.class);
            dtos.add(purchaseInbound);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public PurchaseInbound save(PurchaseInbound purchaseInbound) {
        if (purchaseInbound.getId() != null) {
            //更新
            PurchaseInbound original = purchaseInboundRepository.getById(purchaseInbound.getId());
            BeanUtil.copyProperties(purchaseInbound, original, CopyOptions.create().ignoreNullValue());
            return purchaseInboundRepository.save(original);
        }
        return purchaseInboundRepository.save(purchaseInbound);
    }

    @Transactional
    public void delete(Long purchaseInboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseInbound)
                .where(qPurchaseInbound.id.eq(purchaseInboundId).and(qPurchaseInbound.merchantId.eq(merchantId)).and(qPurchaseInbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseInbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseInbound).where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseInbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseInbound.accountBookId.eq(accountBookId));
            }
        }
    }
}
