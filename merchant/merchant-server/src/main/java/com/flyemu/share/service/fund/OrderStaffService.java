package com.flyemu.share.service.fund;


import cn.hutool.core.bean.BeanUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.entity.fund.OrderStaff;
import com.flyemu.share.entity.fund.QOrderReceipt;
import com.flyemu.share.entity.fund.QOrderStaff;
import com.flyemu.share.repository.OrderReceiptRepository;
import com.flyemu.share.repository.OrderStaffRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务层
 *
 * @author shuaiqi
 * @since 2025-05-20 11:53:48
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderStaffService extends AbsService {
    private final static QOrderStaff orderStaff = QOrderStaff.orderStaff;

    private final OrderStaffRepository orderReceiptRepository;

    public Object query(Page page, OrderStaffService.Query query) {
        PagedList<OrderStaff> fetchPage = bqf.selectFrom(orderStaff).where(query.builder).orderBy(orderStaff.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        List<OrderStaff> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OrderStaff orderReceipt1 = tuple;
            OrderStaff orderReceipt = BeanUtil.toBean(orderReceipt1, OrderStaff.class);
            dtos.add(orderReceipt);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public List<OrderStaff> list(Query query) {
        return bqf.selectFrom(orderStaff)
                .where(query.builder)
                .orderBy(orderStaff.id.desc())
                .fetch();
    }

    public void add(OrderStaff jxcOrderStaff, Long merchantId, Long accountBookId) {
        jxcOrderStaff.setMerchantId(merchantId);
        jxcOrderStaff.setAccountBookId(accountBookId);
        orderReceiptRepository.save(jxcOrderStaff);
    }


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(orderStaff.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(orderStaff.accountBookId.eq(accountBookId));
            }
        }

    }


}

