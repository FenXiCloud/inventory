package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.purchase.PurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.repository.PurchaseOrderRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 销售订单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseOrderService extends AbsService {

    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;

    private final PurchaseOrderRepository purchaseOrderRepository;

    public PageResults<PurchaseOrder> query(Page page, Query query) {
        PagedList<PurchaseOrder> fetchPage = bqf.selectFrom(qPurchaseOrder).where(query.builder).orderBy(qPurchaseOrder.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseOrder> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseOrder purchaseOrder1 = tuple;
            PurchaseOrder purchaseOrder = BeanUtil.toBean(purchaseOrder1, PurchaseOrder.class);
            dtos.add(purchaseOrder);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public PurchaseOrder save(PurchaseOrder purchaseOrder) {
        if (purchaseOrder.getId() != null) {
            //更新
            PurchaseOrder original = purchaseOrderRepository.getById(purchaseOrder.getId());
            BeanUtil.copyProperties(purchaseOrder, original, CopyOptions.create().ignoreNullValue());
            return purchaseOrderRepository.save(original);
        }
        return purchaseOrderRepository.save(purchaseOrder);
    }

    @Transactional
    public void delete(Long purchaseOrderId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseOrder)
                .where(qPurchaseOrder.id.eq(purchaseOrderId).and(qPurchaseOrder.merchantId.eq(merchantId)).and(qPurchaseOrder.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseOrder> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseOrder).where(qPurchaseOrder.merchantId.eq(merchantId).and(qPurchaseOrder.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseOrder.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseOrder.accountBookId.eq(accountBookId));
            }
        }
    }
}
