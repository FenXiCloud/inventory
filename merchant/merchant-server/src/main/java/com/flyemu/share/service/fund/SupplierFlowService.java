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
import com.flyemu.share.dto.SupplierFlowDTO;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.entity.fund.QSupplierFlow;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.entity.setting.QAccountBook;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SupplierInitialForm;
import com.flyemu.share.repository.fund.SupplierFlowRepository;
import com.flyemu.share.repository.basic.SupplierRepository;
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
public class SupplierFlowService extends BaseService {

    private final static QSupplierFlow qSupplierFlow = QSupplierFlow.supplierFlow;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QAccountBook qAccountBook = QAccountBook.accountBook;

    private final SupplierFlowRepository supplierFlowRepository;
    private final SupplierRepository supplierRepository;
    private final CheckoutService checkoutService;

    public List<SupplierFlow> query(Query query) {
        return bqf.selectFrom(qSupplierFlow).where(query.builder).orderBy(qSupplierFlow.id.desc()).fetch();
    }

    public PageResults<SupplierFlow> statement(Page page, SupplierFlowService.QueryDTO queryDTO) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(queryDTO.builder);

        PagedList<SupplierFlow> fetchPage = bqf.selectFrom(qSupplierFlow)
                .select(qSupplierFlow)
                .where(builder)
                .orderBy(
                        Expressions.booleanTemplate("case when {0} = '期初' then true else false end", qSupplierFlow.supplierFlowType).desc(),

                        qSupplierFlow.id.desc()
                )
                .fetchPage(page.getOffset(), page.getPageSize());

        return new PageResults<>(fetchPage, page);
    }

    public PageResults<SupplierFlowDTO> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qSupplierFlow).select(qSupplierFlow, qSupplier.name, qSupplier.code).leftJoin(qSupplier).on(qSupplier.id.eq(qSupplierFlow.supplierId)).where(query.buildersV2()).orderBy(qSupplierFlow.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        ArrayList<SupplierFlowDTO> collect = fetchPage.stream().collect(ArrayList::new, (list, tuple) -> {
            SupplierFlow flow = tuple.get(qSupplierFlow);
            SupplierFlowDTO dto = BeanUtil.toBean(flow, SupplierFlowDTO.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setSupplierCode(tuple.get(qSupplier.code));
            // 期初列表字段：应付 / 预付 / 余额
            dto.setBalanceBefore(flow.getCopeWithAmount());
            dto.setAmount(flow.getActualPaymentAmount());
            dto.setBalanceAfter(flow.getBalancePayable());
            list.add(dto);
        }, List::addAll);

        return new PageResults<>(collect, page, fetchPage.getTotalSize());
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
            throw new ServiceException("账套已结账，不允许设置或修改供应商期初余额");
        }
    }

    @Transactional
    public SupplierFlow save(SupplierFlow supplierFlow) {
        // 结账后不允许设置/修改期初余额
        if (SupplierFlow.SupplierFlowType.期初.equals(supplierFlow.getSupplierFlowType())) {
            assertNotCheckedOut(supplierFlow.getMerchantId(), supplierFlow.getAccountBookId());
        }

        if (supplierFlow.getId() != null) {
            //更新
            SupplierFlow original = supplierFlowRepository.getById(supplierFlow.getId());
            BeanUtil.copyProperties(supplierFlow, original, CopyOptions.create().ignoreNullValue());
            return supplierFlowRepository.save(original);
        }
        return supplierFlowRepository.save(supplierFlow);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        // 结账后不允许删除期初余额
        assertNotCheckedOut(merchantId, accountBookId);

        jqf.delete(qSupplierFlow).where(qSupplierFlow.id.eq(supplierFlowId).and(qSupplierFlow.merchantId.eq(merchantId)).and(qSupplierFlow.accountBookId.eq(accountBookId))).execute();
    }

    public List<SupplierFlow> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSupplierFlow).where(qSupplierFlow.merchantId.eq(merchantId).and(qSupplierFlow.accountBookId.eq(accountBookId))).fetch();
    }

    @Transactional
    public void insert(SupplierFlow form) {
        if (form.getMerchantId() == null) {
            throw new ServiceException("商户ID不能为空");
        }
        if (form.getAccountBookId() == null) {
            throw new ServiceException("账簿ID不能为空");
        }
        if (form.getSupplierId() == null) {
            throw new ServiceException("货商ID不能为空");
        }
        if (form.getSupplierFlowType() == null) {
            throw new ServiceException("单据类型不能为空");
        }
        if (form.getBalancePayable() == null) {
            throw new ServiceException("应付余额不能为空");
        }
        supplierFlowRepository.save(form);
    }

    @Transactional
    public void batch(SupplierInitialForm form) {
        // 结账后不允许设置/修改期初余额
        assertNotCheckedOut(form.getMerchantId(), form.getAccountBookId());

        List<SupplierFlow> supplierFlowList = form.getSupplierFlowList();
        for (SupplierFlow item : supplierFlowList) {
            item.setAccountBookId(form.getAccountBookId());
            item.setMerchantId(form.getMerchantId());
            item.setCreatedBy(form.getCreatedBy());
            item.setCreatedAt(LocalDateTime.now());
            item.setSupplierFlowType(SupplierFlow.SupplierFlowType.期初);
            if (item.getBalancePayable() == null) {
                throw new InvalidContextException("期初余额不能为空");
            }
            if (item.getId() != null) {
                //更新
                SupplierFlow original = supplierFlowRepository.getById(item.getId());
                BeanUtil.copyProperties(item, original, CopyOptions.create().ignoreNullValue());
                supplierFlowRepository.save(original);
            } else {
                //按照商品id和仓库id 查询数据是否存在，组装查询条件
                Specification<SupplierFlow> query = (root, criteriaQuery, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(criteriaBuilder.equal(root.get("supplierId"), item.getSupplierId()));
                    predicates.add(criteriaBuilder.equal(root.get("accountBookId"), item.getAccountBookId()));
                    predicates.add(criteriaBuilder.equal(root.get("merchantId"), item.getMerchantId()));
                    predicates.add(criteriaBuilder.equal(root.get("supplierFlowType"), SupplierFlow.SupplierFlowType.期初));
                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
                //如果存在，则抛出异常
                if (supplierFlowRepository.exists(query)) {
                    //根据供应商id查询供应商
                    Supplier supplier = supplierRepository.getById(item.getSupplierId());
                    throw new InvalidContextException("供应商：" + supplier.getName() + "，期初余额数据已存在");
                }
                //新增
                supplierFlowRepository.save(item);
            }
            jqf.update(qSupplier)
                    .set(qSupplier.balance, item.getBalancePayable())
                    .where(qSupplier.id.eq(item.getSupplierId())
                            .and(qSupplier.merchantId.eq(form.getMerchantId()))
                            .and(qSupplier.accountBookId.eq(form.getAccountBookId())))
                    .execute();
        }
    }

    public SupplierFlowDTO getById(SupplierFlow query) {
        SupplierFlow item = supplierFlowRepository.getById(query.getId());
        SupplierFlowDTO dto = BeanUtil.toBean(item, SupplierFlowDTO.class);
        dto.setBalanceBefore(item.getCopeWithAmount());
        dto.setAmount(item.getActualPaymentAmount());
        dto.setBalanceAfter(item.getBalancePayable());
        return dto;
    }

    @Transactional
    public void batchDelete(List<Long> ids, Long merchantId, Long accountBookId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 结账后不允许删除期初余额
        assertNotCheckedOut(merchantId, accountBookId);

        jqf.delete(qSupplierFlow)
                .where(qSupplierFlow.id.in(ids)
                        .and(qSupplierFlow.merchantId.eq(merchantId))
                        .and(qSupplierFlow.accountBookId.eq(accountBookId)))
                .execute();
    }

    public static class QueryDTO implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSupplierFlow.merchantId, merchantId);
        }

        public void setSupplierId(Long supplierId) {
            builder.and(qSupplierFlow.supplierId.eq(supplierId));
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSupplierFlow.accountBookId, accountBookId);
        }

        public void setStartTime(  LocalDateTime startTime) {
            builder.and(qSupplierFlow.createdAt.goe(startTime));

        }

        public void setEndTime( LocalDateTime endTime) {
            builder.and(qSupplierFlow.createdAt.loe(endTime));
        }
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private String filter;

        private String supplierIds;

        @Enumerated(EnumType.STRING)
        private SupplierFlow.SupplierFlowType supplierFlowType;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSupplierFlow.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSupplierFlow.accountBookId, accountBookId);
        }

        public BooleanBuilder buildersV2() {
            if (supplierFlowType != null) {
                builder.and(qSupplierFlow.supplierFlowType.eq(supplierFlowType));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qSupplier.code.contains(filter).or(qSupplier.name.contains(filter)));
            }
            if (StrUtil.isNotBlank(supplierIds)) {
                builder.and(qSupplierFlow.supplierId.in(Arrays.stream(supplierIds.split(",")).map(Long::parseLong).toList()));
            }
            return builder;
        }

    }
}
