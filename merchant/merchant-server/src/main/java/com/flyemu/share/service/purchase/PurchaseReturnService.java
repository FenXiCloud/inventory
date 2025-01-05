package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.purchase.PurchaseReturn;
import com.flyemu.share.entity.purchase.QPurchaseReturn;
import com.flyemu.share.repository.PurchaseReturnRepository;
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
public class PurchaseReturnService extends AbsService {

    private final static QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;

    private final PurchaseReturnRepository purchaseReturnRepository;

    public PageResults<PurchaseReturn> query(Page page, Query query) {
        PagedList<PurchaseReturn> fetchPage = bqf.selectFrom(qPurchaseReturn).where(query.builder).orderBy(qPurchaseReturn.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseReturn> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReturn purchaseReturn1 = tuple;
            PurchaseReturn purchaseReturn = BeanUtil.toBean(purchaseReturn1, PurchaseReturn.class);
            dtos.add(purchaseReturn);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public PurchaseReturn save(PurchaseReturn purchaseReturn) {
        if (purchaseReturn.getId() != null) {
            //更新
            PurchaseReturn original = purchaseReturnRepository.getById(purchaseReturn.getId());
            BeanUtil.copyProperties(purchaseReturn, original, CopyOptions.create().ignoreNullValue());
            return purchaseReturnRepository.save(original);
        }
        return purchaseReturnRepository.save(purchaseReturn);
    }

    @Transactional
    public void delete(Long PurchaseReturnId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseReturn)
                .where(qPurchaseReturn.id.eq(PurchaseReturnId).and(qPurchaseReturn.merchantId.eq(merchantId)).and(qPurchaseReturn.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseReturn> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseReturn).where(qPurchaseReturn.merchantId.eq(merchantId).and(qPurchaseReturn.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseReturn.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseReturn.accountBookId.eq(accountBookId));
            }
        }
    }
}
