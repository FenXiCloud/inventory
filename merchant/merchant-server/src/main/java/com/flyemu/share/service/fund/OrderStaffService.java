package com.flyemu.share.service.fund;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.lang.Assert;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.fund.OrderStaff;
import com.flyemu.share.entity.fund.QOrderStaff;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.fund.OrderStaffRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 业务员
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class OrderStaffService extends BaseService {

    private static final QOrderStaff qOrderStaff = QOrderStaff.orderStaff;

    private final OrderStaffRepository orderStaffRepository;

    public PageResults<OrderStaff> query(Page page, Query query) {
        PagedList<OrderStaff> fetchPage = bqf.selectFrom(qOrderStaff)
                .where(query.builder)
                .orderBy(qOrderStaff.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        return new PageResults<>(fetchPage, page, fetchPage.getTotalSize());
    }

    public List<OrderStaff> select(Query query) {
        return bqf.selectFrom(qOrderStaff)
                .where(query.builder)
                .orderBy(qOrderStaff.id.desc())
                .fetch();
    }

    public OrderStaff load(Integer id, Long merchantId, Long accountBookId) {
        OrderStaff staff = bqf.selectFrom(qOrderStaff)
                .where(qOrderStaff.id.eq(id)
                        .and(qOrderStaff.merchantId.eq(merchantId))
                        .and(qOrderStaff.accountBookId.eq(accountBookId)))
                .fetchOne();
        Assert.notNull(staff, "业务员不存在");
        return staff;
    }

    @Transactional
    public void save(OrderStaff orderStaff) {
        validate(orderStaff);
        Date now = new Date();
        if (orderStaff.getId() == null) {
            orderStaff.setCreateTime(now);
            orderStaff.setUpdateTime(now);
            orderStaffRepository.save(orderStaff);
            return;
        }
        OrderStaff original = load(orderStaff.getId(), orderStaff.getMerchantId(), orderStaff.getAccountBookId());
        original.setCode(orderStaff.getCode());
        original.setName(orderStaff.getName());
        original.setPhone(orderStaff.getPhone());
        original.setUpdateTime(now);
        orderStaffRepository.save(original);
    }

    @Transactional
    public void delete(Integer id, Long merchantId, Long accountBookId) {
        jqf.delete(qOrderStaff)
                .where(qOrderStaff.id.eq(id)
                        .and(qOrderStaff.merchantId.eq(merchantId))
                        .and(qOrderStaff.accountBookId.eq(accountBookId)))
                .execute();
    }

    private void validate(OrderStaff orderStaff) {
        if (StrUtil.isEmpty(orderStaff.getCode())) {
            throw new ServiceException("请输入编号");
        }
        if (StrUtil.isEmpty(orderStaff.getName())) {
            throw new ServiceException("请输入名称");
        }
        if (StrUtil.isEmpty(orderStaff.getPhone())) {
            throw new ServiceException("请输入手机号");
        }
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qOrderStaff.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qOrderStaff.accountBookId, accountBookId);
        }
    }
}
