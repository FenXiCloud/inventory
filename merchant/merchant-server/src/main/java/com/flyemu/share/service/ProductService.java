package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONObject;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.PinYinUtil;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.ProductDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.repository.CustomerLevelPriceRepository;
import com.flyemu.share.repository.ProductRepository;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @功能描述: 商品列表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ProductService extends AbsService {

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final QUnit qUnit = QUnit.unit;

    private final ProductRepository productRepository;

    private final CustomerLevelPriceRepository customerLevelPriceRepository;

    private final QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;

    private final QCustomer qCustomers = QCustomer.customer;


    public PageResults<ProductDto> query(Page page, Query query) {
        PagedList<Tuple> pagedList = bqf.selectFrom(qProduct)
                .select(qProduct, qUnit.name, qProductCategory.name)
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builders())
                .orderBy(qProduct.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<ProductDto> collect = pagedList.stream().collect(ArrayList::new, (list, tuple) -> {
            ProductDto dto = BeanUtil.toBean(tuple.get(qProduct), ProductDto.class);
            dto.setCategoryName(tuple.get(qProductCategory.name));
            dto.setUnitName(tuple.get(qUnit.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }

    @Transactional
    public void save(ProductForm productForm, Long merchantId, Long accountBookId) {
        Product product = productForm.getProduct();
        if (product.getEnableAuxiliaryUnit()) {
            Assert.isTrue(CollUtil.isNotEmpty(product.getAuxiliaryUnitPrices()), "开启多单位,必须选择一个副单位");
            Set<Long> checkUnit = new HashSet<>();
            checkUnit.add(product.getUnitId());
            for (AuxiliaryUnitPrice up : product.getAuxiliaryUnitPrices()) {
                if (up.getUnitId() != null) {
                    Assert.isFalse(checkUnit.contains(up.getUnitId()), up.getUnitName() + "单位,不能一致");
                }
            }
        }
        if (product.getId() != null) {
            Product original = productRepository.getById(product.getId());

            if (original.getEnableAuxiliaryUnit()) {
                List<Long> unitIds;
                if (!product.getEnableAuxiliaryUnit()) { //关闭多单位需要去检测
                    unitIds = original.getAuxiliaryUnitPrices().stream().map(AuxiliaryUnitPrice::getUnitId).collect(Collectors.toList());
                } else {//取有变动的单位去校验
                    unitIds = new ArrayList<>();
                    if (!original.getAuxiliaryUnitPrices().equals(product.getAuxiliaryUnitPrices())) {
                        List<AuxiliaryUnitPrice> prices = product.getAuxiliaryUnitPrices();
                        original.getAuxiliaryUnitPrices().forEach(item -> {
                            if (!prices.contains(item)) {
                                unitIds.add(item.getUnitId());
                            }
                        });
                    }
                }
            }
            if (!original.getName().equals(product.getName())) {
                original.setPinyin(PinYinUtil.getFirstLettersLo(product.getName()) + "," + PinYinUtil.getPinyinString(product.getName()));
            }
            BeanUtil.copyProperties(product, original, CopyOptions.create().ignoreNullValue());
            product = productRepository.save(original);

        } else {

            product.setAccountBookId(accountBookId);
            product.setMerchantId(merchantId);
            product.setMerchantId(merchantId);
            product.setEnabled(true);
            product.setPinyin(PinYinUtil.getFirstLettersLo(product.getName()) + "," + PinYinUtil.getPinyinString(product.getName()));
            productRepository.save(product);
        }

        if (CollUtil.isNotEmpty(productForm.getLevelPriceList())) {
            List<CustomerLevelPrice> cps = new ArrayList<>();
            for (JSONObject cp : productForm.getLevelPriceList()) {
                CustomerLevelPrice levelPrice = new CustomerLevelPrice();
                levelPrice.setProductId(product.getId());
                levelPrice.setUnitId(product.getUnitId());
                levelPrice.setPrice(cp.getBigDecimal("price"));
                levelPrice.setMerchantId(merchantId);
                levelPrice.setAccountBookId(accountBookId);
                levelPrice.setCustomerLevelId(cp.getLong("customerLeveId"));
                if (product.getEnableAuxiliaryUnit()) {
                    List<AuxiliaryUnitPrice> ups = new ArrayList<>();
                    for (AuxiliaryUnitPrice multiUnit : product.getAuxiliaryUnitPrices()) {
                        AuxiliaryUnitPrice up = new AuxiliaryUnitPrice();
                        up.setUnitName(multiUnit.getUnitName());
                        up.setUnitId(multiUnit.getUnitId());
                        up.setUnitPrice(cp.getBigDecimal(multiUnit.getUnitId() + ""));
                        ups.add(up);
                    }
                    levelPrice.setAuxiliaryUnitPrices(ups);
                }
                cps.add(levelPrice);
            }
            jqf.delete(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(product.getId()).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).
                    execute();
            customerLevelPriceRepository.saveAll(cps);
        } else {
            List<CustomerLevelPrice> priceList = jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(product.getId()).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetch();
            for (CustomerLevelPrice price : priceList) {
                price.setUnitId(product.getUnitId());
                if (product.getEnableAuxiliaryUnit()) {
                    for (int i = 0; i < product.getAuxiliaryUnitPrices().size(); i++) {
                        AuxiliaryUnitPrice gp = product.getAuxiliaryUnitPrices().get(i);
                        if (CollUtil.isNotEmpty(price.getAuxiliaryUnitPrices())) {
                            AuxiliaryUnitPrice auxiliaryUnitPrice = null;
                            if (price.getAuxiliaryUnitPrices() != null && price.getAuxiliaryUnitPrices().size() > i) {
                                auxiliaryUnitPrice = price.getAuxiliaryUnitPrices().get(i);
                            }
                            if (auxiliaryUnitPrice != null) {
                                auxiliaryUnitPrice.setUnitId(gp.getUnitId());
                                auxiliaryUnitPrice.setUnitName(gp.getUnitName());
                                auxiliaryUnitPrice.setConversionRate(gp.getConversionRate());
                            } else {
                                AuxiliaryUnitPrice up = new AuxiliaryUnitPrice();
                                up.setUnitId(gp.getUnitId());
                                up.setUnitName(gp.getUnitName());
                                up.setConversionRate(gp.getConversionRate());
                                up.setUnitPrice(BigDecimal.ZERO);
                                price.getAuxiliaryUnitPrices().add(up);
                            }
                        } else {
                            List<AuxiliaryUnitPrice> ups = new ArrayList<>();
                            for (AuxiliaryUnitPrice multiUnit : product.getAuxiliaryUnitPrices()) {
                                AuxiliaryUnitPrice up = new AuxiliaryUnitPrice();
                                up.setUnitName(multiUnit.getUnitName());
                                up.setUnitId(multiUnit.getUnitId());
                                up.setConversionRate(gp.getConversionRate());
                                up.setUnitPrice(BigDecimal.ZERO);
                                ups.add(up);
                            }
                            price.setAuxiliaryUnitPrices(ups);
                        }
                    }
                }
            }
            if (CollUtil.isNotEmpty(priceList)) {
                customerLevelPriceRepository.saveAll(priceList);
            }
        }
    }

    @Transactional
    public void delete(Long productsId, Long merchantId, Long accountBookId) {
        jqf.delete(qCustomerLevelPrice)
                .where(qCustomerLevelPrice.productId.eq(productsId).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qProduct)
                .where(qProduct.id.eq(productsId).and(qProduct.merchantId.eq(merchantId)).and(qProduct.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Product> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qProduct).where(qProduct.merchantId.eq(merchantId).and(qProduct.accountBookId.eq(accountBookId))
                .and(qProduct.enabled.isTrue())).fetch();
    }


    public Product loadById(Long productId, Long merchantId) {
        return jqf.selectFrom(qProduct).where(qProduct.id.eq(productId).and(qProduct.merchantId.eq(merchantId))).fetchFirst();
    }

    //TODO 命名
    public Map<Long, CustomerLevelPrice> levelPrice(Long productId, Long merchantId, Long accountBookId) {
        return jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(productId).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetch().stream().collect(Collectors.toMap(c -> c.getCustomerLevelId(), b -> b));
    }

    @Data
    public static class Query {

        public final BooleanBuilder builder = new BooleanBuilder();

        private String path;

        private String filter;

        private Integer categoryId;

        private Boolean enabled;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qProduct.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qProduct.accountBookId.eq(accountBookId));
            }
        }

        public void setEnabled(Boolean enabled) {
            if (enabled != null) {
                builder.and(qProduct.enabled.eq(enabled));
            }
        }

        public BooleanBuilder builders() {

            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qProduct.name.contains(filter)
                        .or(qProduct.code.contains(filter))
                        .or(qProduct.pinyin.contains(filter)));
            }
            if (enabled != null) {
                builder.and(qProduct.enabled.eq(enabled));
            }
            return builder;
        }

    }
}
