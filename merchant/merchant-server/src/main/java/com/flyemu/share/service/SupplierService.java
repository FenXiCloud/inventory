package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SupplierDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.SupplierRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 供货商管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierService extends AbsService {

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final static QProduct qProduct = QProduct.product;

    private final static QUnit qUnit = QUnit.unit;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;

    private final SupplierRepository supplierRepository;


    public PageResults query(Page page, Query query) {
        PagedList<Tuple> pagedList = bqf.selectFrom(qSupplier).select(qSupplier, qSupplierCategory.name).leftJoin(qSupplierCategory).on(qSupplier.supplierCategoryId.eq(qSupplierCategory.id)).where(query.builder).orderBy(qSupplier.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<SupplierDto> collect = pagedList.stream().collect(ArrayList::new, (list, tuple) -> {
            SupplierDto dto = BeanUtil.toBean(tuple.get(qSupplier), SupplierDto.class);
            dto.setCategoryName(tuple.get(qSupplierCategory.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }

    @Transactional
    public Supplier save(Supplier supplier) {
        try {
            if (supplier.getId() != null) {
                //更新
                Supplier original = supplierRepository.getById(supplier.getId());
                BeanUtil.copyProperties(supplier, original, CopyOptions.create().ignoreNullValue());
                return supplierRepository.save(original);
            }
            return supplierRepository.save(supplier);
        } catch (Exception e) {
            log.error("supplier save", e);
            throw new ServiceException(e.getMessage());
        }
    }

    @Transactional
    public void delete(Long vendorsId, Long merchantId) {
        jqf.delete(qSupplier).where(qSupplier.id.eq(vendorsId).and(qSupplier.merchantId.eq(merchantId))).execute();
    }

    public List<Supplier> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSupplier).where(qSupplier.merchantId.eq(merchantId).and(qSupplier.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSupplier.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSupplier.accountBookId.eq(accountBookId));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSupplier.code.contains(filter).or(qSupplier.contactPerson.contains(filter)).or(qSupplier.phone.contains(filter)).or(qSupplier.name.contains(filter)));
            }
        }
    }
}
