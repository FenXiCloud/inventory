package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.ProductCategory;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.CategoryTreeRepository;
import com.flyemu.share.repository.basic.ProductCategoryRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductCategoryService extends BaseService {

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
            if (productCategory.getPid() != null) {
                assertCanAddChild(productCategory.getPid(), productCategory.getMerchantId(), productCategory.getAccountBookId());
            }

            if (productCategory.getId() != null) {
                ProductCategory original = productCategoryRepository.getById(productCategory.getId());
                // 编码已隐藏，更新时保留原编码
                String originalCode = original.getCode();
                BeanUtil.copyProperties(productCategory, original, CopyOptions.create().ignoreNullValue());
                original.setCode(originalCode);
                if (productCategory.getPid() == null) {
                    original.setPid(null);
                }
                // leaf 由分类下是否有产品决定，保存分类时不改写
                original.setLeaf(hasProducts(original.getId(), original.getMerchantId(), original.getAccountBookId()));
                return productCategoryRepository.save(original);
            }

            // 前端不再录入编码，后台自动生成
            if (StringUtils.isEmpty(productCategory.getCode())) {
                productCategory.setCode("C" + System.currentTimeMillis());
            } else {
                BooleanBuilder builder = new BooleanBuilder();
                builder.and(qProductCategory.merchantId.eq(productCategory.getMerchantId()))
                        .and(qProductCategory.accountBookId.eq(productCategory.getAccountBookId()))
                        .and(qProductCategory.code.eq(productCategory.getCode()));
                Long count = jqf.select(qProductCategory.id.count())
                        .from(qProductCategory)
                        .where(builder)
                        .fetchOne();
                if (count != null && count > 0) {
                    throw new ServiceException("已存在相同编码的分类：" + productCategory.getCode());
                }
            }

            productCategory.setLeaf(false);
            productCategoryRepository.save(productCategory);
            return productCategory;

        } catch (ServiceException e) {
            throw e;
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

        jqf.delete(qProductCategory)
                .where(qProductCategory.id.eq(productsCategoryId)
                        .and(qProductCategory.merchantId.eq(merchantId))
                        .and(qProductCategory.accountBookId.eq(accountBookId)))
                .execute();
    }

    public ProductCategory loadById(Long merchantId, Long orgId) {
        return bqf.selectFrom(qProductCategory).where(qProductCategory.merchantId.eq(merchantId).and(qProductCategory.id.eq(orgId))).fetchFirst();
    }

    /**
     * 下拉/树数据：leaf 实时按「分类下是否有产品」计算，不用库里旧值。
     */
    public List<ProductCategory> select(Long merchantId, Long accountBookId) {
        List<ProductCategory> list = bqf.selectFrom(qProductCategory)
                .where(qProductCategory.merchantId.eq(merchantId).and(qProductCategory.accountBookId.eq(accountBookId)))
                .orderBy(qProductCategory.sort.desc())
                .fetch();
        Set<Long> withProducts = new HashSet<>(bqf.select(qProduct.productCategoryId)
                .from(qProduct)
                .where(qProduct.merchantId.eq(merchantId)
                        .and(qProduct.accountBookId.eq(accountBookId))
                        .and(qProduct.productCategoryId.isNotNull()))
                .distinct()
                .fetch());
        list.forEach(c -> c.setLeaf(withProducts.contains(c.getId())));
        return list;
    }

    public boolean hasProducts(Long categoryId, Long merchantId, Long accountBookId) {
        if (categoryId == null) {
            return false;
        }
        return bqf.selectFrom(qProduct)
                .where(qProduct.productCategoryId.eq(categoryId)
                        .and(qProduct.merchantId.eq(merchantId))
                        .and(qProduct.accountBookId.eq(accountBookId)))
                .fetchCount() > 0;
    }

    public boolean hasChildren(Long categoryId, Long merchantId, Long accountBookId) {
        if (categoryId == null) {
            return false;
        }
        return bqf.selectFrom(qProductCategory)
                .where(qProductCategory.pid.eq(categoryId)
                        .and(qProductCategory.merchantId.eq(merchantId))
                        .and(qProductCategory.accountBookId.eq(accountBookId)))
                .fetchCount() > 0;
    }

    /** 有产品的分类是末级，不允许再创建下级 */
    public void assertCanAddChild(Long parentId, Long merchantId, Long accountBookId) {
        Assert.notNull(parentId, "父级分类不能为空");
        Assert.isFalse(hasProducts(parentId, merchantId, accountBookId), "该分类下已有产品，属于末级，不能创建下级");
    }

    /** 产品只能挂在无下级的分类上 */
    public void assertCanBindProduct(Long categoryId, Long merchantId, Long accountBookId) {
        Assert.notNull(categoryId, "请选择产品分类");
        Assert.isFalse(hasChildren(categoryId, merchantId, accountBookId), "有下级的分类不能选择，请选择末级分类");
    }

    @Transactional
    public void refreshLeafByProducts(Long categoryId, Long merchantId, Long accountBookId) {
        if (categoryId == null) {
            return;
        }
        boolean leaf = hasProducts(categoryId, merchantId, accountBookId);
        jqf.update(qProductCategory)
                .set(qProductCategory.leaf, leaf)
                .where(qProductCategory.id.eq(categoryId)
                        .and(qProductCategory.merchantId.eq(merchantId))
                        .and(qProductCategory.accountBookId.eq(accountBookId)))
                .execute();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qProductCategory.name.contains(name));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qProductCategory.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qProductCategory.accountBookId, accountBookId);
        }
    }
}
