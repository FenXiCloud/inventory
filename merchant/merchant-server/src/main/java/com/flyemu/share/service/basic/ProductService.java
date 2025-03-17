package com.flyemu.share.service.basic;

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
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.repository.CustomerLevelPriceRepository;
import com.flyemu.share.repository.CustomerLevelRepository;
import com.flyemu.share.repository.ProductRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

    private final PriceRecordService priceRecordService;

    private final CustomerLevelRepository customerLevelRepository;

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
            dto.setProductCategoryName(tuple.get(qProductCategory.name));
            dto.setUnitName(tuple.get(qUnit.name));
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }

    @Transactional
    public void save(ProductForm productForm, Long merchantId, Long accountBookId) {
        Product product = productForm.getProduct();
        if (product.getEnableMultiUnit()) {
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

            if (original.getEnableMultiUnit()) {
                List<Long> unitIds;
                if (!product.getEnableMultiUnit()) { //关闭多单位需要去检测
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
            //保存价格记录
            savePrice(original);
            product = productRepository.save(original);

        } else {

            product.setAccountBookId(accountBookId);
            product.setMerchantId(merchantId);
            product.setMerchantId(merchantId);
            product.setEnabled(true);
            product.setPinyin(PinYinUtil.getFirstLettersLo(product.getName()) + "," + PinYinUtil.getPinyinString(product.getName()));
            productRepository.save(product);
        }

        if (CollUtil.isNotEmpty(productForm.getCustomerLevelPriceList())) {
            List<CustomerLevelPrice> cps = new ArrayList<>();
            for (JSONObject cp : productForm.getCustomerLevelPriceList()) {
                CustomerLevelPrice levelPrice = new CustomerLevelPrice();
                levelPrice.setProductId(product.getId());
                levelPrice.setUnitId(product.getUnitId());
                levelPrice.setPrice(cp.getBigDecimal("price"));
                levelPrice.setMerchantId(merchantId);
                levelPrice.setAccountBookId(accountBookId);
                levelPrice.setCustomerLevelId(cp.getLong("customerLeveId"));
                if (product.getEnableMultiUnit()) {
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
                //保存价格记录
                savePrice(levelPrice);
                cps.add(levelPrice);
            }
            jqf.delete(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(product.getId()).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).
                    execute();
            customerLevelPriceRepository.saveAll(cps);
        } else {
            List<CustomerLevelPrice> priceList = jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(product.getId()).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetch();
            for (CustomerLevelPrice price : priceList) {
                price.setUnitId(product.getUnitId());
                if (product.getEnableMultiUnit()) {
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

    /**
     * 保存预计采购价格
     * @param product
     */
    private void savePrice(Product product) {
        //保存价格记录
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(product.getPurchasePrice());
        priceRecord.setBaseUnitId(product.getUnitId());
        priceRecord.setProductId(product.getId());
        priceRecord.setMerchantId(product.getMerchantId());
        priceRecord.setAccountBookId(product.getAccountBookId());
        priceRecord.setPriceSource(PriceSource.商品价格资料);
        priceRecord.setPriceType(PriceType.预计采购价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    /**
     * 保存客户等级价格
     * @param customerLevelPrice
     */
    private void savePrice(CustomerLevelPrice customerLevelPrice) {
        //保存价格记录
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(customerLevelPrice.getPrice());
        priceRecord.setBaseUnitId(customerLevelPrice.getUnitId());
        priceRecord.setProductId(customerLevelPrice.getProductId());
        priceRecord.setMerchantId(customerLevelPrice.getMerchantId());
        priceRecord.setAccountBookId(customerLevelPrice.getAccountBookId());
        priceRecord.setPriceSource(PriceSource.商品价格资料);
        Long customerLevelId = customerLevelPrice.getCustomerLevelId();
        CustomerLevel customerLevel = customerLevelRepository.getById(customerLevelId);
        String name = customerLevel.getName();

        PriceType priceType = null;
        if(StringUtils.equals(name,"会员价")){
            priceType = PriceType.VIP客户价格;
        }else if(StringUtils.equals(name,"零售价")){
            priceType = PriceType.零售客户价格;
        }else {
            log.info("客户等级价格保存失败：{}",name);
            return;
        }
        priceRecord.setPriceType(priceType);
        priceRecordService.savePriceRecord(priceRecord);
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

    public List<ProductDto> select(Long merchantId, Long accountBookId) {
        //left join 查询商品单位
        List<Tuple> fetch = bqf.selectFrom(qProduct)
                .select(qProduct, qUnit.name)
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId)).where(qProduct.merchantId.eq(merchantId)
                .and(qProduct.accountBookId.eq(accountBookId)).and(qProduct.enabled.isTrue())).fetch();
        //封装产品单位返回;
        ArrayList<ProductDto> result = fetch.stream().collect(ArrayList::new, (list, tuple) -> {
            ProductDto dto = BeanUtil.toBean(tuple.get(qProduct), ProductDto.class);
            dto.setUnitName(tuple.get(qUnit.name));
            list.add(dto);
        }, List::addAll);
        return result;
    }


    public Product loadById(Long productId, Long merchantId) {
        return jqf.selectFrom(qProduct).where(qProduct.id.eq(productId).and(qProduct.merchantId.eq(merchantId))).fetchFirst();
    }

    public Map<Long, CustomerLevelPrice> customerLevelPrice(Long productId, Long merchantId, Long accountBookId) {
        return jqf.selectFrom(qCustomerLevelPrice).where(qCustomerLevelPrice.productId.eq(productId).and(qCustomerLevelPrice.merchantId.eq(merchantId)).and(qCustomerLevelPrice.accountBookId.eq(accountBookId))).fetch().stream().collect(Collectors.toMap(c -> c.getCustomerLevelId(), b -> b));
    }

    @Data
    public static class Query {

        public final BooleanBuilder builder = new BooleanBuilder();

        private String path;

        private String filter;

        private Integer categoryId;

        private Boolean enabled;

        private Long id;

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
            if (id != null) {
                builder.and(qProduct.id.eq(id));
            }
            return builder;
        }

    }
}
