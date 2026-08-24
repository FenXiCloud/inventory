package com.flyemu.share.service.basic;

import com.flyemu.share.entity.basic.*;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.enums.PolicySource;
import com.flyemu.share.enums.PolicyType;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.repository.basic.PricingPolicyRepository;
import com.flyemu.share.service.BaseService;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * 开单价格取数：按价格策略优先级链式回退
 * <p>
 * 销售：客户等级价格 → 最近销售价<br>
 * 采购：最近采购价格 → 产品档案采购价
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PriceResolveService extends BaseService {

    private final static QPricingPolicy qPricingPolicy = QPricingPolicy.pricingPolicy;
    private final static QPriceRecord qPriceRecord = QPriceRecord.priceRecord;
    private final static QCustomerLevelPrice qCustomerLevelPrice = QCustomerLevelPrice.customerLevelPrice;
    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QProduct qProduct = QProduct.product;

    private final PricingPolicyRepository pricingPolicyRepository;

    /**
     * 销售开单取价
     */
    public BigDecimal resolveSalesPrice(Long productId, Long customerId, Long merchantId, Long accountBookId) {
        if (productId == null || merchantId == null || accountBookId == null) {
            return BigDecimal.ZERO;
        }
        List<PricingPolicy> policies = loadEnabledPolicies(PolicyType.销售价格取数, merchantId, accountBookId);
        Long customerLevelId = null;
        if (customerId != null) {
            Customer customer = jqf.selectFrom(qCustomer)
                    .where(qCustomer.id.eq(customerId)
                            .and(qCustomer.merchantId.eq(merchantId))
                            .and(qCustomer.accountBookId.eq(accountBookId)))
                    .fetchFirst();
            if (customer != null) {
                customerLevelId = customer.getCustomerLevelId();
            }
        }
        for (PricingPolicy policy : policies) {
            BigDecimal price = null;
            if (policy.getPolicySource() == PolicySource.客户等级价格) {
                price = findCustomerLevelPrice(productId, customerLevelId, merchantId, accountBookId);
            } else if (policy.getPolicySource() == PolicySource.最近销售单价) {
                price = findLatestTradePrice(productId, PriceType.最近销售价格, PriceSource.最近销售价格, merchantId, accountBookId);
            }
            if (isValidPrice(price)) {
                return price;
            }
        }
        // 策略未命中时兜底：等级价 → 最近销售价
        BigDecimal level = findCustomerLevelPrice(productId, customerLevelId, merchantId, accountBookId);
        if (isValidPrice(level)) {
            return level;
        }
        BigDecimal recent = findLatestTradePrice(productId, PriceType.最近销售价格, PriceSource.最近销售价格, merchantId, accountBookId);
        return isValidPrice(recent) ? recent : BigDecimal.ZERO;
    }

    /**
     * 采购开单取价
     */
    public BigDecimal resolvePurchasePrice(Long productId, Long merchantId, Long accountBookId) {
        if (productId == null || merchantId == null || accountBookId == null) {
            return BigDecimal.ZERO;
        }
        List<PricingPolicy> policies = loadEnabledPolicies(PolicyType.采购价格取数, merchantId, accountBookId);
        for (PricingPolicy policy : policies) {
            BigDecimal price = null;
            if (policy.getPolicySource() == PolicySource.最近采购单价) {
                price = findLatestTradePrice(productId, PriceType.最近采购价格, PriceSource.最近采购价格, merchantId, accountBookId);
            } else if (policy.getPolicySource() == PolicySource.预计采购价格) {
                price = findProductPurchasePrice(productId, merchantId, accountBookId);
            }
            if (isValidPrice(price)) {
                return price;
            }
        }
        // 策略未命中时兜底：最近采购 → 档案采购价
        BigDecimal recent = findLatestTradePrice(productId, PriceType.最近采购价格, PriceSource.最近采购价格, merchantId, accountBookId);
        if (isValidPrice(recent)) {
            return recent;
        }
        BigDecimal archive = findProductPurchasePrice(productId, merchantId, accountBookId);
        return isValidPrice(archive) ? archive : BigDecimal.ZERO;
    }

    private List<PricingPolicy> loadEnabledPolicies(PolicyType policyType, Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPricingPolicy)
                .where(qPricingPolicy.merchantId.eq(merchantId)
                        .and(qPricingPolicy.accountBookId.eq(accountBookId))
                        .and(qPricingPolicy.policyType.eq(policyType))
                        .and(qPricingPolicy.enabled.isTrue()))
                .orderBy(qPricingPolicy.priority.asc())
                .fetch();
    }

    private BigDecimal findCustomerLevelPrice(Long productId, Long customerLevelId, Long merchantId, Long accountBookId) {
        if (customerLevelId == null) {
            return null;
        }
        CustomerLevelPrice levelPrice = jqf.selectFrom(qCustomerLevelPrice)
                .where(qCustomerLevelPrice.productId.eq(productId)
                        .and(qCustomerLevelPrice.customerLevelId.eq(customerLevelId))
                        .and(qCustomerLevelPrice.merchantId.eq(merchantId))
                        .and(qCustomerLevelPrice.accountBookId.eq(accountBookId)))
                .orderBy(qCustomerLevelPrice.id.desc())
                .fetchFirst();
        return levelPrice != null ? levelPrice.getPrice() : null;
    }

    private BigDecimal findLatestTradePrice(Long productId, PriceType priceType, PriceSource priceSource,
                                           Long merchantId, Long accountBookId) {
        PriceRecord record = jqf.selectFrom(qPriceRecord)
                .where(qPriceRecord.productId.eq(productId)
                        .and(qPriceRecord.priceType.eq(priceType))
                        .and(qPriceRecord.priceSource.eq(priceSource))
                        .and(qPriceRecord.merchantId.eq(merchantId))
                        .and(qPriceRecord.accountBookId.eq(accountBookId)))
                .orderBy(qPriceRecord.id.desc())
                .fetchFirst();
        if (record == null) return null;
        // PriceRecord.unitPrice 是基本单位单价；如果产品启用了辅助单位，需要换算为采购单位单价
        return toPurchaseUnitPrice(productId, merchantId, accountBookId, record.getUnitPrice());
    }

    /**
     * 将基本单位单价换算为采购单位单价（如果产品启用了辅助单位）。
     * 换算逻辑：找到 conversionRate > 1 的辅助单位条目（即采购单位），用基本单价 × 换算率。
     */
    private BigDecimal toPurchaseUnitPrice(Long productId, Long merchantId, Long accountBookId, BigDecimal baseUnitPrice) {
        Product product = jqf.selectFrom(qProduct)
                .where(qProduct.id.eq(productId)
                        .and(qProduct.merchantId.eq(merchantId))
                        .and(qProduct.accountBookId.eq(accountBookId)))
                .fetchFirst();
        if (product == null || !Boolean.TRUE.equals(product.getEnableMultiUnit())
                || CollUtil.isEmpty(product.getAuxiliaryUnitPrices())) {
            return baseUnitPrice;
        }
        // 找 conversionRate > 1 的条目（采购单位），用最大换算率
        Optional<AuxiliaryUnitPrice> purchaseUnit = product.getAuxiliaryUnitPrices().stream()
                .filter(a -> a.getConversionRate() != null && a.getConversionRate() > 1)
                .max((a, b) -> Double.compare(a.getConversionRate(), b.getConversionRate()));
        if (purchaseUnit.isEmpty()) {
            return baseUnitPrice;
        }
        BigDecimal rate = BigDecimal.valueOf(purchaseUnit.get().getConversionRate());
        return baseUnitPrice.multiply(rate).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal findProductPurchasePrice(Long productId, Long merchantId, Long accountBookId) {
        Product product = jqf.selectFrom(qProduct)
                .where(qProduct.id.eq(productId)
                        .and(qProduct.merchantId.eq(merchantId))
                        .and(qProduct.accountBookId.eq(accountBookId)))
                .fetchFirst();
        return product != null ? product.getPurchasePrice() : null;
    }

    private boolean isValidPrice(BigDecimal price) {
        return price != null && price.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 确保账套有默认销售/采购取数规则
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureDefaultPolicies(Long merchantId, Long accountBookId) {
        if (merchantId == null || accountBookId == null) {
            return;
        }
        long salesCount = bqf.selectFrom(qPricingPolicy)
                .where(qPricingPolicy.merchantId.eq(merchantId)
                        .and(qPricingPolicy.accountBookId.eq(accountBookId))
                        .and(qPricingPolicy.policyType.eq(PolicyType.销售价格取数)))
                .fetchCount();
        if (salesCount == 0) {
            savePolicy(merchantId, accountBookId, PolicyType.销售价格取数, PolicySource.客户等级价格, 1, "优先取客户对应等级价格");
            savePolicy(merchantId, accountBookId, PolicyType.销售价格取数, PolicySource.最近销售单价, 2, "无等级价时取最近销售出库成交价");
        }
        long purchaseCount = bqf.selectFrom(qPricingPolicy)
                .where(qPricingPolicy.merchantId.eq(merchantId)
                        .and(qPricingPolicy.accountBookId.eq(accountBookId))
                        .and(qPricingPolicy.policyType.eq(PolicyType.采购价格取数)))
                .fetchCount();
        if (purchaseCount == 0) {
            savePolicy(merchantId, accountBookId, PolicyType.采购价格取数, PolicySource.预计采购价格, 1, "优先取产品档案默认采购价");
            savePolicy(merchantId, accountBookId, PolicyType.采购价格取数, PolicySource.最近采购单价, 2, "无档案价时取最近采购入库成交价");
        }
    }

    private void savePolicy(Long merchantId, Long accountBookId, PolicyType type, PolicySource source,
                            int priority, String remarks) {
        PricingPolicy policy = new PricingPolicy();
        policy.setMerchantId(merchantId);
        policy.setAccountBookId(accountBookId);
        policy.setPolicyType(type);
        policy.setPolicySource(source);
        policy.setPriority(priority);
        policy.setEnabled(true);
        policy.setRemarks(remarks);
        pricingPolicyRepository.save(policy);
    }
}
