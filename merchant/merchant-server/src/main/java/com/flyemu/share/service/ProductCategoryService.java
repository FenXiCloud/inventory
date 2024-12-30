package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.entity.basic.ProductCategory;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.ProductCategoryRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 商品分类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductCategoryService extends AbsService {

    private static final QProductCategory qProductCategory = QProductCategory.productCategory;

    private final ProductCategoryRepository productCategoryRepository;

    private static final QProduct qProduct = QProduct.product;

    public List<ProductCategory> query(Query query) {
        return bqf.selectFrom(qProductCategory).where(query.builder).orderBy(qProductCategory.sort.desc(), qProductCategory.code.asc(), qProductCategory.id.asc()).fetch();
    }

    @Transactional
    public ProductCategory save(ProductCategory productCategory) {
        try {
            String path = "";
            if (productCategory.getPid() != null) {
                ProductCategory parent = productCategoryRepository.getReferenceById(productCategory.getPid());
                parent.setLeaf(false);
                productCategoryRepository.save(parent);
            }

            if (productCategory.getId() != null) {
                //更新
                ProductCategory original = productCategoryRepository.getById(productCategory.getId());

                BeanUtil.copyProperties(productCategory, original, CopyOptions.create().ignoreNullValue());
                if (productCategory.getPid() == null) {
                    original.setPid(null);
                    original.setLeaf(true);
                }
                return productCategoryRepository.save(original);
            }

            productCategory.setLeaf(true);
            productCategoryRepository.save(productCategory);
            return productCategory;
        } catch (Exception e) {
            log.error("ProductCategory", e);
            throw new ServiceException(e.getMessage());
        }
    }

    @Transactional
    public void delete(Long merchantId, Long productsCategoryId, Long accountBookId) {
        ProductCategory productCategory = productCategoryRepository.getReferenceById(productsCategoryId);

        List<Long> ids = bqf.selectFrom(qProductCategory).select(qProductCategory.id).where((qProductCategory.merchantId.eq(merchantId)).and(qProductCategory.accountBookId.eq(accountBookId))).fetch();

        Assert.isFalse(bqf.selectFrom(qProduct).where(qProduct.categoryId.in(ids).and(qProduct.merchantId.eq(merchantId)).and(qProduct.accountBookId.eq(accountBookId))).fetchCount() > 0, "商品已使用，不能删除");

        if (productCategory.getPid() != null) {
            long count = bqf.selectFrom(qProductCategory).where(qProductCategory.pid.eq(productCategory.getPid()).and(qProductCategory.merchantId.eq(merchantId)).and(qProductCategory.accountBookId.eq(accountBookId))).fetchCount();
            if (count == 1) {
                jqf.update(qProductCategory).set(qProductCategory.leaf, true).where(qProductCategory.id.eq(productCategory.getPid()).and(qProductCategory.merchantId.eq(merchantId)).and(qProductCategory.accountBookId.eq(accountBookId))).execute();
            }
        }
        jqf.delete(qProductCategory).where(qProductCategory.merchantId.eq(merchantId).and(qProductCategory.accountBookId.eq(accountBookId)).and(qProductCategory.id.in(ids))).execute();
    }

    public ProductCategory loadById(Long merchantId, Long orgId) {
        return bqf.selectFrom(qProductCategory).where(qProductCategory.merchantId.eq(merchantId).and(qProductCategory.id.eq(orgId))).fetchFirst();
    }

    public List<ProductCategory> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qProductCategory).where(qProductCategory.merchantId.eq(merchantId).and(qProductCategory.accountBookId.eq(accountBookId))).orderBy(qProductCategory.sort.desc()).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qProductCategory.name.contains(name));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qProductCategory.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qProductCategory.accountBookId.eq(accountBookId));
            }
        }
    }
}
