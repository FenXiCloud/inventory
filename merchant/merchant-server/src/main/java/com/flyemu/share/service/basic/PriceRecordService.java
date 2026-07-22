package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson2.JSON;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.PinYinUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.ProductDto;
import com.flyemu.share.dto.ProductPriceDTO;
import com.flyemu.share.dto.price.PriceRecordDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.enums.PolicySource;
import com.flyemu.share.enums.PolicyType;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.repository.basic.CustomerLevelPriceRepository;
import com.flyemu.share.repository.basic.PriceRecordRepository;
import com.flyemu.share.repository.basic.PricingPolicyRepository;
import com.flyemu.share.repository.basic.ProductRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.flyemu.share.enums.PolicySource.*;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceRecordService extends BaseService {

    private final static QPriceRecord qPriceRecord = QPriceRecord.priceRecord;

    private final PriceRecordRepository priceRecordRepository;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QProduct qProduct = QProduct.product;
    private static final QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QUnit qUnit = QUnit.unit;

    private final ProductRepository productRepository;
    private final CustomerLevelPriceRepository customerLevelPriceRepository;
    private final PricingPolicyRepository pricingPolicyRepository;
    private final static QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;

    public PageResults<PriceRecordDto> query(Page page, Query query) {

        PagedList<Tuple> fetchPage = bqf.selectFrom(qPriceRecord)
                .select(qPriceRecord, qProduct.name, qProduct.code, qProduct.specification, qProductCategory.id, qProductCategory.name,
                        qUnit.name)
                .innerJoin(qProduct).on(qProduct.id.eq(qPriceRecord.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPriceRecord.baseUnitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builder)
                .orderBy(qPriceRecord.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        List<PriceRecordDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PriceRecordDto priceRecordDTO = BeanUtil.toBean(tuple.get(qPriceRecord), PriceRecordDto.class);
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

    /**
     * 成交价流水：每次审核写入一条，不依赖价格策略，不做同价去重。
     */
    @Transactional
    public void appendTradePrice(PriceRecord priceRecord) {
        if (priceRecord == null || priceRecord.getProductId() == null || priceRecord.getUnitPrice() == null) {
            log.warn("成交价记录参数不完整，跳过写入");
            return;
        }
        this.save(priceRecord);
    }

    /**
     * 反审核时按单据删除对应成交价流水
     */
    @Transactional
    public void removeByOrder(Long orderId, PriceType priceType, PriceSource priceSource, Long merchantId, Long accountBookId) {
        if (orderId == null || merchantId == null || accountBookId == null) {
            return;
        }
        BooleanBuilder where = new BooleanBuilder();
        where.and(qPriceRecord.orderId.eq(orderId));
        where.and(qPriceRecord.merchantId.eq(merchantId));
        where.and(qPriceRecord.accountBookId.eq(accountBookId));
        if (priceType != null) {
            where.and(qPriceRecord.priceType.eq(priceType));
        }
        if (priceSource != null) {
            where.and(qPriceRecord.priceSource.eq(priceSource));
        }
        jqf.delete(qPriceRecord).where(where).execute();
    }

    @Transactional
    public void savePriceRecord(PriceRecord priceRecord) {
        if (priceRecord == null) {
            log.info("价格记录不能未空");
            return;
        }
        // 成交价（最近采购/最近销售）始终追加，不依赖策略、不去重
        if (priceRecord.getPriceType() == PriceType.最近采购价格
                || priceRecord.getPriceType() == PriceType.最近销售价格) {
            this.appendTradePrice(priceRecord);
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
            PriceType priceType = priceRecord.getPriceType();
            if (priceType != null) {
                predicates.add(cb.equal(root.get("priceType"), priceType));
            }
            PriceSource priceSource = priceRecord.getPriceSource();
            if (priceSource != null) {
                predicates.add(cb.equal(root.get("priceSource"), priceSource));
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
            if (dbPriceRecord.getUnitPrice().compareTo(priceRecord.getUnitPrice()) == 0) {
                //不更新
            } else {
                //更新价格和单位
                this.save(priceRecord);
            }
        }
    }

    /**
     * 产品价格资料列表：默认采购价 + 各客户等级价
     */
    public PageResults<ProductPriceDTO> productList(Page page, ProductService.Query query) {
        PagedList<Tuple> pagedList = bqf.selectFrom(qProduct)
                .select(qProduct, qUnit.name, qProductCategory.name)
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builders())
                .orderBy(qProduct.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<ProductPriceDTO> rows = new ArrayList<>();
        List<Long> productIds = new ArrayList<>();
        for (Tuple tuple : pagedList) {
            Product product = tuple.get(qProduct);
            ProductPriceDTO dto = new ProductPriceDTO();
            dto.setId(product.getId());
            dto.setCode(product.getCode());
            dto.setName(product.getName());
            dto.setSpecification(product.getSpecification());
            dto.setPurchasePrice(product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO);
            dto.setUnitId(product.getUnitId());
            dto.setUnitName(tuple.get(qUnit.name));
            dto.setProductCategoryId(product.getProductCategoryId());
            dto.setProductCategoryName(tuple.get(qProductCategory.name));
            dto.setAccountBookId(product.getAccountBookId());
            dto.setMerchantId(product.getMerchantId());
            rows.add(dto);
            productIds.add(product.getId());
        }

        if (!productIds.isEmpty() && query != null) {
            Long merchantId = rows.get(0).getMerchantId();
            Long accountBookId = rows.get(0).getAccountBookId();
            List<CustomerLevelPrice> levelPriceList = jqf.selectFrom(qCustomerLevelPrice)
                    .where(qCustomerLevelPrice.productId.in(productIds)
                            .and(qCustomerLevelPrice.merchantId.eq(merchantId))
                            .and(qCustomerLevelPrice.accountBookId.eq(accountBookId)))
                    .fetch();
            Map<Long, Map<Long, BigDecimal>> byProduct = new HashMap<>();
            for (CustomerLevelPrice lp : levelPriceList) {
                byProduct
                        .computeIfAbsent(lp.getProductId(), k -> new HashMap<>())
                        .put(lp.getCustomerLevelId(), lp.getPrice() != null ? lp.getPrice() : BigDecimal.ZERO);
            }
            for (ProductPriceDTO row : rows) {
                row.setLevelPrices(byProduct.getOrDefault(row.getId(), new HashMap<>()));
            }
        }
        return new PageResults<>(rows, page, pagedList.getTotalSize());
    }

    /**
     * 单元格保存：默认采购价或客户等级价
     */
    @Transactional
    public void productCellSave(com.flyemu.share.form.ProductPriceCellForm form, Long merchantId, Long accountBookId) {
        if (form.getPrice() == null || form.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new com.flyemu.share.exception.ServiceException("价格不能为负数");
        }
        Product dbProduct = productRepository.getById(form.getProductId());
        if (!Objects.equals(dbProduct.getAccountBookId(), accountBookId)
                || !Objects.equals(dbProduct.getMerchantId(), merchantId)) {
            throw new com.flyemu.share.exception.ServiceException("产品不存在或不属于当前账套");
        }

        if ("purchasePrice".equals(form.getField())) {
            dbProduct.setPurchasePrice(form.getPrice());
            productRepository.save(dbProduct);
            return;
        }

        if ("levelPrice".equals(form.getField())) {
            if (form.getCustomerLevelId() == null) {
                throw new com.flyemu.share.exception.ServiceException("客户等级不能为空");
            }
            CustomerLevelPrice existing = jqf.selectFrom(qCustomerLevelPrice)
                    .where(qCustomerLevelPrice.productId.eq(form.getProductId())
                            .and(qCustomerLevelPrice.customerLevelId.eq(form.getCustomerLevelId())
                                    .and(qCustomerLevelPrice.merchantId.eq(merchantId))
                                    .and(qCustomerLevelPrice.accountBookId.eq(accountBookId))))
                    .fetchFirst();
            if (existing != null) {
                existing.setPrice(form.getPrice());
                existing.setUnitId(dbProduct.getUnitId());
                customerLevelPriceRepository.save(existing);
            } else {
                CustomerLevelPrice created = new CustomerLevelPrice();
                created.setProductId(form.getProductId());
                created.setCustomerLevelId(form.getCustomerLevelId());
                created.setUnitId(dbProduct.getUnitId());
                created.setPrice(form.getPrice());
                created.setMerchantId(merchantId);
                created.setAccountBookId(accountBookId);
                customerLevelPriceRepository.save(created);
            }
            return;
        }
        throw new com.flyemu.share.exception.ServiceException("不支持的字段：" + form.getField());
    }

    @Transactional
    public void productSave(ProductForm productForm, Long merchantId, Long accountBookId) {
        Product product = productForm.getProduct();
        Long id = product.getId();
        Product dbProduct = productRepository.getById(id);
        if (!Objects.equals(dbProduct.getAccountBookId(), accountBookId)) {
            throw new RuntimeException("accountBookId错误");
        }
        if (!Objects.equals(dbProduct.getMerchantId(), merchantId)) {
            throw new RuntimeException("merchantId错误");
        }
        BigDecimal purchasePrice = product.getPurchasePrice();
        if (purchasePrice != null && purchasePrice.compareTo(BigDecimal.ZERO) >= 0) {
            dbProduct.setPurchasePrice(purchasePrice);
        }
        productRepository.save(dbProduct);

        if (CollUtil.isNotEmpty(productForm.getCustomerLevelPriceList())) {
            List<CustomerLevelPrice> cps = new ArrayList<>();
            for (JSONObject cp : productForm.getCustomerLevelPriceList()) {
                CustomerLevelPrice levelPrice = new CustomerLevelPrice();
                levelPrice.setProductId(product.getId());
                levelPrice.setUnitId(dbProduct.getUnitId());
                levelPrice.setPrice(cp.getBigDecimal("price"));
                levelPrice.setMerchantId(merchantId);
                levelPrice.setAccountBookId(accountBookId);
                levelPrice.setCustomerLevelId(cp.getLong("customerLeveId") != null
                        ? cp.getLong("customerLeveId")
                        : cp.getLong("customerLevelId"));
                cps.add(levelPrice);
            }
            jqf.delete(qCustomerLevelPrice)
                    .where(qCustomerLevelPrice.productId.eq(product.getId())
                            .and(qCustomerLevelPrice.merchantId.eq(merchantId))
                            .and(qCustomerLevelPrice.accountBookId.eq(accountBookId)))
                    .execute();
            customerLevelPriceRepository.saveAll(cps);
        }
    }

    public PageResults<PriceRecordDto> showPrice(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPriceRecord)
                .select(qPriceRecord, qProduct.name, qProduct.code, qProduct.specification)
                .leftJoin(qProduct).on(qProduct.id.eq(qPriceRecord.productId))
                .where(query.builder)
                .orderBy(qPriceRecord.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        List<PriceRecordDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PriceRecordDto priceRecordDTO = BeanUtil.toBean(tuple.get(qPriceRecord), PriceRecordDto.class);
            priceRecordDTO.setProductName(tuple.get(qProduct.name));
            priceRecordDTO.setProductCode(tuple.get(qProduct.code));
            priceRecordDTO.setSpecification(tuple.get(qProduct.specification));
            dtos.add(priceRecordDTO);
        });
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public List<PriceRecordDto> showPurchasePrice(Query query) {
        List<Tuple> fetchPage = bqf.selectFrom(qPriceRecord)
                .select(qPriceRecord, qProduct.name, qProduct.code, qProduct.specification, qSupplier.name, qProduct.purchasePrice)
                .leftJoin(qProduct).on(qProduct.id.eq(qPriceRecord.productId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPriceRecord.supplierId))
                .where(query.builder.and(qPriceRecord.priceType.eq(PriceType.最近采购价格))
                        .and(qPriceRecord.priceSource.eq(PriceSource.最近采购价格)))
                .orderBy(qPriceRecord.id.desc())
                .limit(5).fetch();
        List<PriceRecordDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PriceRecordDto priceRecordDTO = BeanUtil.toBean(tuple.get(qPriceRecord), PriceRecordDto.class);
            priceRecordDTO.setProductName(tuple.get(qProduct.name));
            priceRecordDTO.setProductCode(tuple.get(qProduct.code));
            priceRecordDTO.setSupplierName(tuple.get(qSupplier.name));
            priceRecordDTO.setSpecification(tuple.get(qProduct.specification));
            priceRecordDTO.setPurchasePrice(tuple.get(qProduct.purchasePrice));
            dtos.add(priceRecordDTO);
        });
        return dtos;
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPriceRecord.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPriceRecord.accountBookId, accountBookId);
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

        public void setProductIds(String productIds) {
            if (StrUtil.isNotBlank(productIds)) {
                builder.and(qPriceRecord.productId.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).collect(Collectors.toList())));
            }
        }

        public void setProductCategoryIds(String productCategoryIds) {
            if (StrUtil.isNotBlank(productCategoryIds)) {
                builder.and(qProductCategory.id.in(Arrays.stream(productCategoryIds.split(",")).map(Long::parseLong).collect(Collectors.toList())));
            }
        }

        public void setPriceType(String priceType) {
            if (StrUtil.isNotBlank(priceType)) {
                builder.and(qPriceRecord.priceType.eq(PriceType.valueOf(priceType)));
            }
        }

        public void setPriceSource(String priceSource) {
            if (StrUtil.isNotBlank(priceSource)) {
                builder.and(qPriceRecord.priceSource.eq(PriceSource.valueOf(priceSource)));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qProduct.name.contains(filter).or(qProduct.code.contains(filter)));
            }
        }
    }
}
