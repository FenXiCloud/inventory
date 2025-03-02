package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson2.JSON;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.PinYinUtil;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.ProductDto;
import com.flyemu.share.dto.ProductPriceDTO;
import com.flyemu.share.dto.price.PriceRecordDTO;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.repository.PriceRecordRepository;
import com.flyemu.share.repository.PricingPolicyRepository;
import com.flyemu.share.repository.ProductRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @功能描述: 价格记录表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceRecordService extends AbsService {

    private final static QPriceRecord qPriceRecord = QPriceRecord.priceRecord;

    private final PriceRecordRepository priceRecordRepository;
    private final static QProduct qProduct = QProduct.product;
    private static final QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QUnit qUnit = QUnit.unit;

    private final ProductRepository productRepository;

    public PageResults<PriceRecordDTO> query(Page page, Query query) {

        long totalSize = bqf.selectFrom(qPriceRecord)
                .where(query.builder)
                .fetchCount();
        List<Tuple> fetchPage = bqf.selectFrom(qPriceRecord)
                .select(qPriceRecord, qProduct.name, qProduct.code, qProduct.specification, qProductCategory.name,
                        qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPriceRecord.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPriceRecord.baseUnitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builder)
                .orderBy(qPriceRecord.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();
        List<PriceRecordDTO> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PriceRecordDTO priceRecordDTO = BeanUtil.toBean(tuple.get(qPriceRecord), PriceRecordDTO.class);
            priceRecordDTO.setProductName(tuple.get(qProduct.name));
            priceRecordDTO.setProductCode(tuple.get(qProduct.code));
            priceRecordDTO.setSpecification(tuple.get(qProduct.specification));
            priceRecordDTO.setProductCategory(tuple.get(qProductCategory.name));
            priceRecordDTO.setUnitName(tuple.get(qUnit.name));
            dtos.add(priceRecordDTO);
        });
        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public PriceRecord save(PriceRecord priceRecord) {
        if (priceRecord.getId() != null) {
            //更新
            PriceRecord original = priceRecordRepository.getById(priceRecord.getId());
            BeanUtil.copyProperties(priceRecord, original, CopyOptions.create().ignoreNullValue());
            return priceRecordRepository.save(original);
        }
        return priceRecordRepository.save(priceRecord);
    }

    @Transactional
    public void delete(Long priceRecordId, Long merchantId, Long accountBookId) {
        jqf.delete(qPriceRecord)
                .where(qPriceRecord.id.eq(priceRecordId).and(qPriceRecord.merchantId.eq(merchantId)).and(qPriceRecord.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PriceRecord> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPriceRecord).where(qPriceRecord.merchantId.eq(merchantId).and(qPriceRecord.accountBookId.eq(accountBookId))).fetch();
    }

    private final PricingPolicyRepository pricingPolicyRepository;

    @Transactional
    public void savePriceRecord(PriceRecord priceRecord) {
        if (priceRecord == null) {
            log.info("价格记录不能未空");
            return;
        }
        if (priceRecord.getProductId() == null) {
            log.info("未找到产品ID");
            return;
        }

        if (priceRecord.getOrderId() == null) {
            log.info("未找到单据ID");
            return;
        }
        //价格策略查询
        Specification<PricingPolicy> pricingPolicySpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), priceRecord.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), priceRecord.getAccountBookId()));
            predicates.add(cb.equal(root.get("enabled"), true));
            if (priceRecord.getPriceType() != null) {
                predicates.add(cb.equal(root.get("priceType"), priceRecord.getPriceType()));
            }
            if (priceRecord.getPriceSource() != null) {
                predicates.add(cb.equal(root.get("priceSource"), priceRecord.getPriceSource()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<PricingPolicy> pricingPolicyList = pricingPolicyRepository.findAll(pricingPolicySpecification);
        if (CollectionUtils.isEmpty(pricingPolicyList)) {
            log.info("未找到价格策略");
            return;
        }

        AtomicBoolean flag = new AtomicBoolean(false);
        pricingPolicyList.forEach(pricingPolicy -> {
            if (pricingPolicy.getPriceType() == priceRecord.getPriceType() && pricingPolicy.getPriceSource() == priceRecord.getPriceSource()){
                flag.set(true);
            }
        });
        if (!flag.get()) {
            log.info("未找匹配到价格策略");
            return;
        }

        //单据价格记录查询
        Specification<PriceRecord> priceRecordSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), priceRecord.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), priceRecord.getAccountBookId()));
            predicates.add(cb.equal(root.get("productId"), priceRecord.getProductId()));
            predicates.add(cb.equal(root.get("orderId"), priceRecord.getOrderId()));
            Long customerId = priceRecord.getCustomerId();
            if (customerId != null) {
                predicates.add(cb.equal(root.get("customerId"), priceRecord.getCustomerId()));
            }
            Long supplierId = priceRecord.getSupplierId();
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplierId"), priceRecord.getSupplierId()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<PriceRecord> priceRecordList = priceRecordRepository.findAll(priceRecordSpecification);

        log.info("价格记录：{}", JSON.toJSONString(priceRecord));
        if (CollectionUtils.isEmpty(priceRecordList)) {
            this.save(priceRecord);
        } else {
            PriceRecord dbPriceRecord = priceRecordList.get(0);
            //更新价格和单位
            dbPriceRecord.setUnitPrice(priceRecord.getUnitPrice());
            dbPriceRecord.setBaseUnitId(priceRecord.getBaseUnitId());
            this.save(priceRecord);
        }
    }

    /**
     * 产品价格资料
     * @param page
     * @param query
     * @return
     */
    public PageResults<ProductPriceDTO> productList(Page page, ProductService.Query query) {
        PagedList<Tuple> pagedList = bqf.selectFrom(qProduct)
                .select(qProduct, qUnit.name, qProductCategory.name)
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builders())
                .orderBy(qProduct.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<ProductPriceDTO> collect = pagedList.stream().collect(ArrayList::new, (list, tuple) -> {
            ProductPriceDTO dto = BeanUtil.toBean(tuple.get(qProduct), ProductPriceDTO.class);
            dto.setProductCategoryName(tuple.get(qProductCategory.name));
            dto.setUnitName(tuple.get(qUnit.name));
            //最近采购价格
            BigDecimal recentlyPurchasePrice = getLastPrice(dto, PriceType.采购价格取数, PriceSource.最近采购单价);
            dto.setRecentlyPurchasePrice(recentlyPurchasePrice);
            //最近销售价格
            BigDecimal recentlySalesPrice = getLastPrice(dto, PriceType.销售价格取数, PriceSource.最近销售单价);
            dto.setRecentlySalesPrice(recentlySalesPrice);
            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }

    private BigDecimal getLastPrice(ProductPriceDTO dto,PriceType priceType, PriceSource priceSource) {
        Specification<PriceRecord> priceRecordSpecification = (root, rootQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), dto.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), dto.getAccountBookId()));
            predicates.add(cb.equal(root.get("productId"), dto.getId()));
            predicates.add(cb.equal(root.get("priceType"), priceType));
            predicates.add(cb.equal(root.get("priceSource"), priceSource));
            rootQuery.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(0, 1);
        List<PriceRecord> priceRecordList = priceRecordRepository.findAll(priceRecordSpecification, pageRequest).getContent();
        if (CollectionUtils.isEmpty(priceRecordList)) {
            return BigDecimal.ZERO;
        } else {
            PriceRecord priceRecord = priceRecordList.get(0);
            return priceRecord.getUnitPrice();
        }
    }

    @Transactional
    public void productSave(ProductForm productForm, Long merchantId, Long accountBookId) {

        Product product = productForm.getProduct();
        Long id = product.getId();
        Product dbProduct = productRepository.getById(id);
        if(!Objects.equals(dbProduct.getAccountBookId(), accountBookId)){
            throw new RuntimeException("accountBookId错误");
        }
        if(!Objects.equals(dbProduct.getMerchantId(), merchantId)){
            throw new RuntimeException("merchantId错误");
        }
        BigDecimal purchasePrice = product.getPurchasePrice();
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) >= 0) {
            dbProduct.setPurchasePrice(purchasePrice);
        }
        BigDecimal maxPurchasePrice = product.getMaxPurchasePrice();
        if (maxPurchasePrice != null && maxPurchasePrice.compareTo(BigDecimal.ZERO) >= 0) {
            dbProduct.setMaxPurchasePrice(maxPurchasePrice);
        }
        BigDecimal retailCustomerPrice = product.getRetailCustomerPrice();
        if (retailCustomerPrice != null && retailCustomerPrice.compareTo(BigDecimal.ZERO) >= 0){
            dbProduct.setRetailCustomerPrice(retailCustomerPrice);
        }
        BigDecimal wholesaleCustomerPrice = product.getWholesaleCustomerPrice();
        if (wholesaleCustomerPrice != null && wholesaleCustomerPrice.compareTo(BigDecimal.ZERO) >= 0){
            dbProduct.setWholesaleCustomerPrice(wholesaleCustomerPrice);
        }
        BigDecimal vipCustomerPrice = product.getVipCustomerPrice();
        if (vipCustomerPrice != null && vipCustomerPrice.compareTo(BigDecimal.ZERO) >= 0){
            dbProduct.setVipCustomerPrice(vipCustomerPrice);
        }
        BigDecimal minSalesPrice = product.getMinSalesPrice();
        if (minSalesPrice != null && minSalesPrice.compareTo(BigDecimal.ZERO) >= 0){
            dbProduct.setMinSalesPrice(minSalesPrice);
        }
        log.info("产品价格修改：{}", JSON.toJSONString(dbProduct));
        productRepository.save(dbProduct);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPriceRecord.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPriceRecord.accountBookId.eq(accountBookId));
            }
        }

        public void setProductId(Long productId) {
            if (productId != null) {
                builder.and(qPriceRecord.productId.eq(productId));
            }
        }

        public void setPriceType(String priceType) {
            if (StringUtils.isNotBlank(priceType)) {
                builder.and(qPriceRecord.priceType.eq(PriceType.valueOf(priceType)));
            }
        }

        public void setPriceSource(String priceSource) {
            if (StringUtils.isNotBlank(priceSource)) {
                builder.and(qPriceRecord.priceSource.eq(PriceSource.valueOf(priceSource)));
            }
        }
    }
}
