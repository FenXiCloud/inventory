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
import com.flyemu.share.enums.PolicySource;
import com.flyemu.share.enums.PolicyType;
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
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.flyemu.share.enums.PolicySource.*;

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

        PagedList<Tuple> fetchPage = bqf.selectFrom(qPriceRecord)
                .select(qPriceRecord, qProduct.name, qProduct.code, qProduct.specification, qProductCategory.id,qProductCategory.name,
                        qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPriceRecord.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPriceRecord.baseUnitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builder)
                .orderBy(qPriceRecord.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
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
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
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
        //价格策略查询
        Specification<PricingPolicy> pricingPolicySpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), priceRecord.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), priceRecord.getAccountBookId()));
            predicates.add(cb.equal(root.get("enabled"), true));
            PriceType priceType = priceRecord.getPriceType();
            if (priceType != null) {
                if (priceType == PriceType.零售客户价格 || priceType == PriceType.批发客户价格 || priceType == PriceType.VIP客户价格) {
                    predicates.add(cb.equal(root.get("policySource"), PolicySource.客户等级价格));
                } else if (priceType == PriceType.预计采购价格) {
                    predicates.add(cb.equal(root.get("policySource"), PolicySource.预计采购价格));
                } else if (priceType == PriceType.最近销售价格) {
                    predicates.add(cb.equal(root.get("policySource"), PolicySource.最近销售单价));
                } else if (priceType == PriceType.最近采购价格) {
                    predicates.add(cb.equal(root.get("policySource"), PolicySource.最近采购单价));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<PricingPolicy> pricingPolicyList = pricingPolicyRepository.findAll(pricingPolicySpecification);
        if (CollectionUtils.isEmpty(pricingPolicyList)) {
            log.info("未找到价格策略");
            return;
        }
        //单据价格记录查询
        Specification<PriceRecord> priceRecordSpecification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), priceRecord.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), priceRecord.getAccountBookId()));
            predicates.add(cb.equal(root.get("productId"), priceRecord.getProductId()));
//            Long customerId = priceRecord.getCustomerId();
//            if (customerId != null) {
//                predicates.add(cb.equal(root.get("customerId"), customerId));
//            }
//            Long supplierId = priceRecord.getSupplierId();
//            if (supplierId != null) {
//                predicates.add(cb.equal(root.get("supplierId"), supplierId));
//            }
            PriceType priceType = priceRecord.getPriceType();
            if (priceType != null) {
                predicates.add(cb.equal(root.get("priceType"), priceType));
            }
            PriceSource priceSource = priceRecord.getPriceSource();
            if (priceSource != null) {
                predicates.add(cb.equal(root.get("priceSource"), priceType));
            }
            query.orderBy(cb.desc(root.get("id")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        PageRequest pageRequest = PageRequest.of(0, 1);
        List<PriceRecord> priceRecordList = priceRecordRepository.findAll(priceRecordSpecification, pageRequest).getContent();

        log.info("价格记录：{}", JSON.toJSONString(priceRecord));
        if (CollectionUtils.isEmpty(priceRecordList)) {
            this.save(priceRecord);
        } else {
            //取最新的数据
            PriceRecord dbPriceRecord = priceRecordList.get(0);
            if(dbPriceRecord.getUnitPrice().compareTo(priceRecord.getUnitPrice()) == 0){
                //不更新
            }else{
                //更新价格和单位
                this.save(priceRecord);
            }
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
            BigDecimal recentlyPurchasePrice = getLastPrice(dto, PriceSource.最近采购价格, PriceType.最近采购价格);
            dto.setRecentlyPurchasePrice(recentlyPurchasePrice);
            //最近销售价格
            BigDecimal recentlySalesPrice = getLastPrice(dto, PriceSource.最近销售价格, PriceType.最近销售价格);
            dto.setRecentlySalesPrice(recentlySalesPrice);
            //vip价格
            BigDecimal vipCustomerPrice = getLastPrice(dto, PriceSource.商品价格资料, PriceType.VIP客户价格);
            dto.setVipCustomerPrice(vipCustomerPrice);
            //零售客户价格
            BigDecimal retailCustomerPrice = getLastPrice(dto, PriceSource.商品价格资料, PriceType.零售客户价格);
            dto.setRetailCustomerPrice(retailCustomerPrice);
            //批发客户价格
            BigDecimal wholesaleCustomerPrice = getLastPrice(dto, PriceSource.商品价格资料, PriceType.批发客户价格);
            dto.setWholesaleCustomerPrice(wholesaleCustomerPrice);

            // 设置最高采购价格和最低销售价格
            dto.setMaxPurchasePrice(getMaxPurchasePrice(dto));
            dto.setMinSalesPrice(getMinSalesPrice(dto));

            list.add(dto);
        }, List::addAll);
        return new PageResults<>(collect, page, pagedList.getTotalSize());
    }


    private BigDecimal getMaxPurchasePrice(ProductPriceDTO dto) {
        Specification<PriceRecord> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), dto.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), dto.getAccountBookId()));
            predicates.add(cb.equal(root.get("productId"), dto.getId()));
            predicates.add(cb.equal(root.get("priceSource"), PriceSource.最近采购价格));
            predicates.add(cb.equal(root.get("priceType"), PriceType.最近采购价格));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return priceRecordRepository.findAll(specification)
                .stream()
                .map(PriceRecord::getUnitPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getMinSalesPrice(ProductPriceDTO dto) {
        Specification<PriceRecord> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), dto.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), dto.getAccountBookId()));
            predicates.add(cb.equal(root.get("productId"), dto.getId()));
            predicates.add(cb.equal(root.get("priceSource"), PriceSource.最近销售价格));
            predicates.add(cb.equal(root.get("priceType"), PriceType.最近销售价格));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return priceRecordRepository.findAll(specification)
                .stream()
                .map(PriceRecord::getUnitPrice)
                .filter(price -> price.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal getLastPrice(ProductPriceDTO dto,PriceSource priceSource,PriceType priceType) {
        Specification<PriceRecord> priceRecordSpecification = (root, rootQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("merchantId"), dto.getMerchantId()));
            predicates.add(cb.equal(root.get("accountBookId"), dto.getAccountBookId()));
            predicates.add(cb.equal(root.get("productId"), dto.getId()));
            predicates.add(cb.equal(root.get("priceSource"), priceSource));
            predicates.add(cb.equal(root.get("priceType"), priceType));
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

    public PageResults<PriceRecordDTO> showPrice(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPriceRecord)
                .select(qPriceRecord, qProduct.name, qProduct.code, qProduct.specification)
                .leftJoin(qProduct).on(qProduct.id.eq(qPriceRecord.productId))
                .where(query.builder)
                .orderBy(qPriceRecord.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        List<PriceRecordDTO> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PriceRecordDTO priceRecordDTO = BeanUtil.toBean(tuple.get(qPriceRecord), PriceRecordDTO.class);
            priceRecordDTO.setProductName(tuple.get(qProduct.name));
            priceRecordDTO.setProductCode(tuple.get(qProduct.code));
            priceRecordDTO.setSpecification(tuple.get(qProduct.specification));
            dtos.add(priceRecordDTO);
        });
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
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

        public void setProductCategoryId(Long productCategoryId) {
            if (productCategoryId != null) {
                builder.and(qProductCategory.id.eq(productCategoryId));
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

        public void setFilter(String filter) {
            if (StringUtils.isNotBlank(filter)) {
                builder.and(qProduct.name.contains(filter).or(qProduct.code.contains(filter)));
            }
        }
    }
}
