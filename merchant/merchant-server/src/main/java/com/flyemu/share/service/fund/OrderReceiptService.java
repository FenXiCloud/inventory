package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.entity.fund.QOrderReceipt;
import com.flyemu.share.repository.OrderReceiptRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 收款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderReceiptService extends AbsService {

    private final static QOrderReceipt qOrderReceipt = QOrderReceipt.orderReceipt;

    private final OrderReceiptRepository orderReceiptRepository;

    public PageResults<OrderReceipt> query(Page page, OrderReceiptService.Query query) {
        PagedList<OrderReceipt> fetchPage = bqf.selectFrom(qOrderReceipt).where(query.builder).orderBy(qOrderReceipt.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OrderReceipt> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OrderReceipt orderReceipt1 = tuple;
            OrderReceipt orderReceipt = BeanUtil.toBean(orderReceipt1, OrderReceipt.class);
            dtos.add(orderReceipt);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OrderReceipt save(OrderReceipt orderReceipt) {
        if (orderReceipt.getId() != null) {
            //更新
            OrderReceipt original = orderReceiptRepository.getById(orderReceipt.getId());
            BeanUtil.copyProperties(orderReceipt, original, CopyOptions.create().ignoreNullValue());
            return orderReceiptRepository.save(original);
        }
        return orderReceiptRepository.save(orderReceipt);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qOrderReceipt)
                .where(qOrderReceipt.id.eq(supplierFlowId).and(qOrderReceipt.merchantId.eq(merchantId)).and(qOrderReceipt.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OrderReceipt> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOrderReceipt).where(qOrderReceipt.merchantId.eq(merchantId).and(qOrderReceipt.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOrderReceipt.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOrderReceipt.accountBookId.eq(accountBookId));
            }
        }

    }
}
