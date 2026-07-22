package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.QSupplierCategory;
import com.flyemu.share.entity.basic.SupplierCategory;
import com.flyemu.share.repository.basic.SupplierCategoryRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierCategoryService extends BaseService {

    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final SupplierCategoryRepository supplierCategoryRepository;

    public List<SupplierCategory> query(Query query) {
        return bqf.selectFrom(qSupplierCategory).where(query.builder).orderBy(qSupplierCategory.id.desc()).fetch();
    }

    @Transactional
    public SupplierCategory save(SupplierCategory supplierCategory) {
        if (supplierCategory.getId() != null) {
            //更新
            SupplierCategory original = supplierCategoryRepository.getById(supplierCategory.getId());
            BeanUtil.copyProperties(supplierCategory, original, CopyOptions.create().ignoreNullValue());
            return supplierCategoryRepository.save(original);
        }
        return supplierCategoryRepository.save(supplierCategory);
    }

    @Transactional
    public void delete(Long vendorsCategoryId, Long merchantId, Long accountBookId) {
        Assert.isFalse(bqf.selectFrom(qSupplier).where(qSupplier.supplierCategoryId.eq(vendorsCategoryId).and(qSupplier.merchantId.eq(merchantId)).and(qSupplier.accountBookId.eq(accountBookId))).fetchCount() > 0, "分类已使用，不能删除");
        jqf.delete(qSupplierCategory).where(qSupplierCategory.id.eq(vendorsCategoryId).and(qSupplierCategory.merchantId.eq(merchantId)).and(qSupplierCategory.accountBookId.eq(accountBookId))).execute();
    }

    public List<SupplierCategory> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSupplierCategory).where(qSupplierCategory.merchantId.eq(merchantId).and(qSupplierCategory.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSupplierCategory.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSupplierCategory.accountBookId, accountBookId);
        }
    }
}
