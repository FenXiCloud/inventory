package com.flyemu.share.service.basic;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson2.JSON;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.price.PriceRecordDTO;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.repository.PriceRecordRepository;
import com.flyemu.share.repository.PricingPolicyRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
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
    }
}
