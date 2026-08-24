package com.flyemu.share.service.fund;

import com.flyemu.share.common.TenantAware;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.CustomerFlowDTO;
import com.flyemu.share.dto.InventoryItemDTO;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.Warehouse;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.entity.fund.QCustomerFlow;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.form.CustomerInitialForm;
import com.flyemu.share.form.InventoryInitialForm;
import com.flyemu.share.repository.fund.CustomerFlowRepository;
import com.flyemu.share.repository.basic.CustomerRepository;
import com.flyemu.share.entity.setting.QAccountBook;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerFlowService extends BaseService {

    private final static QCustomerFlow qCustomerFlow = QCustomerFlow.customerFlow;
    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QAccountBook qAccountBook = QAccountBook.accountBook;

    private final CustomerFlowRepository customerFlowRepository;
    private final CustomerRepository customerRepository;
    private final CheckoutService checkoutService;

    public PageResults<CustomerFlow> statement(Page page, CustomerBillQueryDTO queryDTO) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(queryDTO.builder);

        PagedList<CustomerFlow> fetchPage = bqf.selectFrom(qCustomerFlow)
                .where(builder)
                .orderBy(
                        Expressions.booleanTemplate("case when {0} = '期初' then true else false end", qCustomerFlow.customerFlowType).desc(),
                        qCustomerFlow.id.desc()
                )
                .fetchPage(page.getOffset(), page.getPageSize());

        return new PageResults<>(fetchPage, page);
    }

    /**
     * 校验账套是否已结账，结账后不允许设置/修改期初余额
     */
    private void assertNotCheckedOut(Long merchantId, Long accountBookId) {
        LocalDate checkoutDate = jqf.select(qAccountBook.checkoutDate)
                .from(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .fetchOne();
        if (checkoutDate != null) {
            throw new ServiceException("账套已结账，不允许设置或修改客户期初余额");
        }
    }

    @Data
    public static class CustomerBillQueryDTO implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCustomerFlow.merchantId, merchantId);
        }

        public void setCustomerId(Long customerId) {
            builder.and(qCustomerFlow.customerId.eq(customerId));
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCustomerFlow.accountBookId, accountBookId);
        }

        public void setStartTime(LocalDateTime startTime) {
            builder.and(qCustomerFlow.createdAt.goe(startTime));
        }

        public void setEndTime(LocalDateTime endTime) {
            builder.and(qCustomerFlow.createdAt.loe(endTime));
        }
    }

    public List<CustomerFlow> query(Query query) {
        return bqf.selectFrom(qCustomerFlow)
                .where(query.builder)
                .orderBy(qCustomerFlow.id.desc())
                .fetch();
    }

    @Transactional
    public CustomerFlow save(CustomerFlow customerFlow) {
        // 结账后不允许设置/修改期初余额
        if (CustomerFlow.CustomerFlowType.期初.equals(customerFlow.getCustomerFlowType())) {
            assertNotCheckedOut(customerFlow.getMerchantId(), customerFlow.getAccountBookId());
        }

        if (customerFlow.getId() != null) {
            //更新
            CustomerFlow original = customerFlowRepository.getById(customerFlow.getId());
            BeanUtil.copyProperties(customerFlow, original, CopyOptions.create().ignoreNullValue());
            return customerFlowRepository.save(original);
        }
        return customerFlowRepository.save(customerFlow);
    }

    @Transactional
    public void delete(Long customerFlowId, Long merchantId, Long accountBookId) {
        // 结账后不允许删除期初余额
        assertNotCheckedOut(merchantId, accountBookId);

        jqf.delete(qCustomerFlow)
                .where(qCustomerFlow.id.eq(customerFlowId).and(qCustomerFlow.merchantId.eq(merchantId)).and(qCustomerFlow.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<CustomerFlow> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomerFlow).where(qCustomerFlow.merchantId.eq(merchantId).and(qCustomerFlow.accountBookId.eq(accountBookId))).fetch();
    }

    public PageResults<CustomerFlowDTO> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qCustomerFlow)
                .select(qCustomerFlow, qCustomer.name, qCustomer.code)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qCustomerFlow.customerId))
                .where(query.buildersV2())
                .orderBy(qCustomerFlow.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        ArrayList<CustomerFlowDTO> collect = fetchPage.stream().collect(ArrayList::new, (list, tuple) -> {
            CustomerFlow flow = tuple.get(qCustomerFlow);
            CustomerFlowDTO dto = BeanUtil.toBean(flow, CustomerFlowDTO.class);
            dto.setCustomerName(tuple.get(qCustomer.name));
            dto.setCustomerCode(tuple.get(qCustomer.code));
            // 期初列表字段：应收 / 预收 / 余额
            dto.setBalanceBefore(flow.getReceivableAmount());
            dto.setAmount(flow.getPaidUpAmount());
            dto.setBalanceAfter(flow.getBalanceReceivables());
            list.add(dto);
        }, List::addAll);

        return new PageResults<>(collect, page, fetchPage.getTotalSize());
    }

    @Transactional
    public void batch(CustomerInitialForm form) {
        // 结账后不允许设置/修改期初余额
        assertNotCheckedOut(form.getMerchantId(), form.getAccountBookId());

        List<CustomerFlow> customerFlowList = form.getCustomerFlowList();
        for (CustomerFlow item : customerFlowList) {
            item.setAccountBookId(form.getAccountBookId());
            item.setMerchantId(form.getMerchantId());
            item.setCreatedBy(form.getCreatedBy());
            item.setCreatedAt(LocalDateTime.now());
            item.setCustomerFlowType(CustomerFlow.CustomerFlowType.期初);
            if (item.getBalanceReceivables() == null) {
                throw new InvalidContextException("期初余额不能为空");
            }
            if (item.getId() != null) {
                //更新
                CustomerFlow original = customerFlowRepository.getById(item.getId());
                BeanUtil.copyProperties(item, original, CopyOptions.create().ignoreNullValue());
                customerFlowRepository.save(original);
            } else {
                //按照商品id和仓库id 查询数据是否存在，组装查询条件
                Specification<CustomerFlow> query = (root, criteriaQuery, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(criteriaBuilder.equal(root.get("customerId"), item.getCustomerId()));
                    predicates.add(criteriaBuilder.equal(root.get("accountBookId"), item.getAccountBookId()));
                    predicates.add(criteriaBuilder.equal(root.get("merchantId"), item.getMerchantId()));
                    predicates.add(criteriaBuilder.equal(root.get("customerFlowType"), CustomerFlow.CustomerFlowType.期初));
                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
                //如果存在，则抛出异常
                if (customerFlowRepository.exists(query)) {
                    //根据商品id查询客户
                    Customer customer = customerRepository.getById(item.getCustomerId());
                    throw new InvalidContextException("客户：" + customer.getName() + "，期初余额数据已存在");
                }
                //新增
                customerFlowRepository.save(item);
            }
            jqf.update(qCustomer)
                    .set(qCustomer.balance, item.getBalanceReceivables())
                    .where(qCustomer.id.eq(item.getCustomerId())
                            .and(qCustomer.merchantId.eq(form.getMerchantId()))
                            .and(qCustomer.accountBookId.eq(form.getAccountBookId())))
                    .execute();
        }
    }

    public CustomerFlowDTO getById(InventoryItem query) {
        CustomerFlow item = customerFlowRepository.getById(query.getId());
        CustomerFlowDTO dto = BeanUtil.toBean(item, CustomerFlowDTO.class);
        dto.setBalanceBefore(item.getReceivableAmount());
        dto.setAmount(item.getPaidUpAmount());
        dto.setBalanceAfter(item.getBalanceReceivables());
        return dto;
    }

    @Transactional
    public void batchDelete(List<Long> ids, Long merchantId, Long accountBookId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 结账后不允许删除期初余额
        assertNotCheckedOut(merchantId, accountBookId);

        jqf.delete(qCustomerFlow)
                .where(qCustomerFlow.id.in(ids)
                        .and(qCustomerFlow.merchantId.eq(merchantId))
                        .and(qCustomerFlow.accountBookId.eq(accountBookId)))
                .execute();
    }

    public void insert(CustomerFlow form) {
        if (form.getMerchantId() == null) {
            throw new InvalidContextException("商户ID不能为空");
        }
        if (form.getAccountBookId() == null) {
            throw new InvalidContextException("账簿ID不能为空");
        }
        if (form.getCustomerId() == null) {
            throw new InvalidContextException("客户ID不能为空");
        }
        if (form.getCustomerFlowType() == null) {
            throw new InvalidContextException("单据类型不能为空");
        }
        if (form.getBalanceReceivables() == null) {
            throw new InvalidContextException("应付余额不能为空");
        }
        customerFlowRepository.save(form);
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private String filter;

        private String customerIds;

        @Enumerated(EnumType.STRING)
        private CustomerFlow.CustomerFlowType customerFlowType;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCustomerFlow.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCustomerFlow.accountBookId, accountBookId);
        }

        public BooleanBuilder buildersV2() {

            if (customerFlowType != null) {
                builder.and(qCustomerFlow.customerFlowType.eq(customerFlowType));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qCustomer.code.contains(filter).or(qCustomer.name.contains(filter)));
            }

            if (StrUtil.isNotBlank(customerIds)) {
                builder.and(qCustomerFlow.customerId.in(Arrays.stream(customerIds.split(",")).map(Long::parseLong).toList()));
            }
            return builder;
        }

    }
}
