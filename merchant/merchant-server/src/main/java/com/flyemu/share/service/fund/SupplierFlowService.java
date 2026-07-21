package com.flyemu.share.service.fund;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SupplierFlowDTO;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.entity.fund.QSupplierFlow;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SupplierInitialForm;
import com.flyemu.share.repository.SupplierFlowRepository;
import com.flyemu.share.repository.SupplierRepository;
import com.flyemu.share.service.AbsService;
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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @功能描述: 供货商交易流水
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierFlowService extends AbsService {

    private final static QSupplierFlow qSupplierFlow = QSupplierFlow.supplierFlow;
    private final static QSupplier qSupplier = QSupplier.supplier;

    private final SupplierFlowRepository supplierFlowRepository;
    private final SupplierRepository supplierRepository;

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
            SupplierFlowDTO dto = BeanUtil.toBean(tuple.get(qSupplierFlow), SupplierFlowDTO.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setSupplierCode(tuple.get(qSupplier.code));
            list.add(dto);
        }, List::addAll);

        return new PageResults<>(collect, page, fetchPage.getTotalSize());
    }

    @Transactional
    public SupplierFlow save(SupplierFlow supplierFlow) {
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
        List<SupplierFlow> supplierFlowList = form.getSupplierFlowList();
        for (SupplierFlow item : supplierFlowList) {
            item.setAccountBookId(form.getAccountBookId());
            item.setMerchantId(form.getMerchantId());
            item.setCreatedBy(form.getCreatedBy());
            item.setCreatedAt(LocalDateTime.now());
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
        }
    }

    public SupplierFlowDTO getById(SupplierFlow query) {
        SupplierFlow item = supplierFlowRepository.getById(query.getId());
        return BeanUtil.toBean(item, SupplierFlowDTO.class);
    }

    @Transactional
    public void batchDelete(List<Long> ids, Long merchantId, Long accountBookId) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        jqf.delete(qSupplierFlow)
                .where(qSupplierFlow.id.in(ids)
                        .and(qSupplierFlow.merchantId.eq(merchantId))
                        .and(qSupplierFlow.accountBookId.eq(accountBookId)))
                .execute();
    }

    public static class QueryDTO {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSupplierFlow.merchantId.eq(merchantId));
            }
        }

        public void setSupplierId(Long supplierId) {
            builder.and(qSupplierFlow.supplierId.eq(supplierId));
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSupplierFlow.accountBookId.eq(accountBookId));
            }
        }

        public void setStartTime(  LocalDateTime startTime) {
            builder.and(qSupplierFlow.createdAt.goe(startTime));

        }

        public void setEndTime( LocalDateTime endTime) {
            builder.and(qSupplierFlow.createdAt.loe(endTime));
        }
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private String filter;

        private String supplierIds;

        @Enumerated(EnumType.STRING)
        private SupplierFlow.SupplierFlowType supplierFlowType;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSupplierFlow.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSupplierFlow.accountBookId.eq(accountBookId));
            }
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
