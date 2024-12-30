package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.QSupplier;
import com.flyemu.share.entity.QSupplierCategory;
import com.flyemu.share.entity.SupplierCategory;
import com.flyemu.share.repository.SupplierCategoryRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 货商分类管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierCategoryService extends AbsService {

    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final SupplierCategoryRepository supplierCategoryRepository;


    public List<SupplierCategory> query(Query query) {
        List<SupplierCategory> supplierCategories = bqf.selectFrom(qSupplierCategory).where(query.builder).orderBy(qSupplierCategory.id.desc()).fetch();
        return supplierCategories;
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


    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSupplierCategory.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSupplierCategory.accountBookId.eq(accountBookId));
            }
        }
    }
}
