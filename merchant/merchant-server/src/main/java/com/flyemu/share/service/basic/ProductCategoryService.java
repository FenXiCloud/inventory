package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.entity.basic.ProductCategory;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CategoryTreeRepository;
import com.flyemu.share.repository.ProductCategoryRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.rowset.serial.SerialException;
import java.util.*;
import java.util.stream.Collectors;

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
    private final CategoryTreeRepository categoryTreeRepository;

    private static final QProduct qProduct = QProduct.product;

    public List<ProductCategory> query(Query query) {
        return bqf.selectFrom(qProductCategory).where(query.builder).orderBy(qProductCategory.sort.desc(), qProductCategory.code.asc(), qProductCategory.id.asc()).fetch();
    }

    @Transactional
    public ProductCategory save(ProductCategory productCategory) {

        try {
            if (StringUtils.isEmpty(productCategory.getCode())){
                throw new SerialException("编码不能为空");
            }
            String path = "";
            if (productCategory.getPid() != null) {
                ProductCategory parent = productCategoryRepository.getReferenceById(productCategory.getPid());
                parent.setLeaf(false);
                productCategoryRepository.save(parent);
            }
            BooleanBuilder builder = new BooleanBuilder();
            builder.and(qProductCategory.merchantId.eq(productCategory.getMerchantId()))
                    .and(qProductCategory.accountBookId.eq(productCategory.getAccountBookId()))
                    .and(qProductCategory.code.eq(productCategory.getCode()));

            if (productCategory.getId() != null) {
                builder.and(qProductCategory.id.ne(productCategory.getId()));
            }

            Long count = jqf.select(qProductCategory.id.count())
                    .from(qProductCategory)
                    .where(builder)
                    .fetchOne();

            if (count != null && count > 0) {
                throw new ServiceException("已存在相同编码的分类：" + productCategory.getCode());
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
        List<Long> allSubCategoryIds = categoryTreeRepository.getAllSubCategoryIds(productsCategoryId);
        List<Long> categoryIds = bqf.select(qProductCategory.id)
                .from(qProductCategory)
                .where(qProductCategory.merchantId.eq(merchantId)
                        .and(qProductCategory.accountBookId.eq(accountBookId))
                        .and(qProductCategory.id.in(allSubCategoryIds)))
                .fetch();
        if (CollUtil.isNotEmpty(categoryIds)) {
            long count = bqf.selectFrom(qProduct)
                    .where(qProduct.productCategoryId.in(categoryIds)
                            .and(qProduct.merchantId.eq(merchantId))
                            .and(qProduct.accountBookId.eq(accountBookId)))
                    .fetchCount();
            Assert.isFalse(count > 0, "该分类或其子分类已被商品使用，不能删除");
        }

        if (productCategory.getPid() != null) {
            long childCount = bqf.selectFrom(qProductCategory)
                    .where(qProductCategory.pid.eq(productCategory.getPid())
                            .and(qProductCategory.merchantId.eq(merchantId))
                            .and(qProductCategory.accountBookId.eq(accountBookId)))
                    .fetchCount();

            if (childCount == 1) {
                jqf.update(qProductCategory)
                        .set(qProductCategory.leaf, true)
                        .where(qProductCategory.id.eq(productCategory.getPid())
                                .and(qProductCategory.merchantId.eq(merchantId))
                                .and(qProductCategory.accountBookId.eq(accountBookId)))
                        .execute();
            }
        }

        jqf.delete(qProductCategory)
                .where(qProductCategory.id.eq(productsCategoryId)
                        .and(qProductCategory.merchantId.eq(merchantId))
                        .and(qProductCategory.accountBookId.eq(accountBookId)))
                .execute();
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
