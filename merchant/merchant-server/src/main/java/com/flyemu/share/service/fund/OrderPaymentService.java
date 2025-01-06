package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.OrderPayment;
import com.flyemu.share.entity.fund.QOrderPayment;
import com.flyemu.share.repository.OrderPaymentRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 付款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderPaymentService extends AbsService {

    private final static QOrderPayment qOrderPayment = QOrderPayment.orderPayment;

    private final OrderPaymentRepository orderPaymentRepository;

    public PageResults<OrderPayment> query(Page page, OrderPaymentService.Query query) {
        PagedList<OrderPayment> fetchPage = bqf.selectFrom(qOrderPayment).where(query.builder).orderBy(qOrderPayment.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OrderPayment> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OrderPayment orderPayment1 = tuple;
            OrderPayment orderPayment = BeanUtil.toBean(orderPayment1, OrderPayment.class);
            dtos.add(orderPayment);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OrderPayment save(OrderPayment orderPayment) {
        if (orderPayment.getId() != null) {
            //更新
            OrderPayment original = orderPaymentRepository.getById(orderPayment.getId());
            BeanUtil.copyProperties(orderPayment, original, CopyOptions.create().ignoreNullValue());
            return orderPaymentRepository.save(original);
        }
        return orderPaymentRepository.save(orderPayment);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qOrderPayment)
                .where(qOrderPayment.id.eq(supplierFlowId).and(qOrderPayment.merchantId.eq(merchantId)).and(qOrderPayment.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OrderPayment> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOrderPayment).where(qOrderPayment.merchantId.eq(merchantId).and(qOrderPayment.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOrderPayment.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOrderPayment.accountBookId.eq(accountBookId));
            }
        }

    }
}
