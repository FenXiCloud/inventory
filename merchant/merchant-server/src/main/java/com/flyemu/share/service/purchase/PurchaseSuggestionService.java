package com.flyemu.share.service.purchase;

import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrderItem;
import com.flyemu.share.entity.purchase.PurchaseOrder;
import com.flyemu.share.entity.purchase.PurchaseOrderItem;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.QSalesOutboundItem;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.repository.purchase.PurchaseOrderItemRepository;
import com.flyemu.share.repository.purchase.PurchaseOrderRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.flyemu.share.exception.ServiceException;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 采购建议：以销定购看板 + 智能补货。
 * 基于近期销量、当前库存、采购在途、安全库存，计算建议采购量，并可一键生成采购订单草稿。
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseSuggestionService extends BaseService {

    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final static QUnit qUnit = QUnit.unit;
    private final static QInventory qInventory = QInventory.inventory;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;

    private final CodeSeedService codeSeedService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static int intAt(Tuple t, int index) {
        Number n = t.get(index, Number.class);
        return n == null ? 0 : n.intValue();
    }

    private static int ceilInt(BigDecimal v) {
        if (v == null || v.signum() <= 0) return 0;
        return v.setScale(0, RoundingMode.CEILING).intValue();
    }

    /**
     * 采购在途量：已审核且未生成入库单的采购订单数量，按商品汇总。
     */
    private Map<Long, BigDecimal> onOrderMap(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(qPurchaseOrderItem.productId, qPurchaseOrderItem.quantity.sum())
                .from(qPurchaseOrderItem)
                .leftJoin(qPurchaseOrder).on(qPurchaseOrder.id.eq(qPurchaseOrderItem.purchaseOrderId))
                .where(qPurchaseOrderItem.merchantId.eq(merchantId)
                        .and(qPurchaseOrderItem.accountBookId.eq(accountBookId))
                        .and(qPurchaseOrder.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseOrder.purchaseInboundId.isNull()))
                .groupBy(qPurchaseOrderItem.productId)
                .fetch();
        Map<Long, BigDecimal> map = new HashMap<>();
        for (Tuple t : tuples) {
            map.put(t.get(qPurchaseOrderItem.productId), nz(t.get(qPurchaseOrderItem.quantity.sum())));
        }
        return map;
    }

    /**
     * 以销定购看板：近 N 天销量 vs 当前库存 + 在途，建议采购量 = 近 N 天销量 − 当前库存 − 在途（向下取整 0）。
     */
    public List<Map<String, Object>> salesDriven(Long merchantId, Long accountBookId, Integer days) {
        int n = days == null || days <= 0 ? 30 : days;
        LocalDate startDate = LocalDate.now().minusDays(n);

        // 当前库存（按商品汇总）
        List<Tuple> currentTuples = jqf.select(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProduct.brand, qProductCategory.name, qUnit.name, qInventory.currentQuantity.sum(),
                        qProduct.alertQuantity, qProduct.maxStockQuantity)
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .where(qInventory.merchantId.eq(merchantId).and(qInventory.accountBookId.eq(accountBookId)))
                .groupBy(qProduct.id, qProduct.code, qProduct.name, qProduct.specification, qProduct.brand,
                        qProductCategory.name, qUnit.name, qProduct.alertQuantity, qProduct.maxStockQuantity)
                .fetch();

        // 近 N 天销量（已审核销售出库）
        List<Tuple> salesTuples = jqf.select(qSalesOutboundItem.productId, qSalesOutboundItem.quantity.sum())
                .from(qSalesOutboundItem)
                .leftJoin(qSalesOutbound).on(qSalesOutbound.id.eq(qSalesOutboundItem.salesOutboundId))
                .where(qSalesOutboundItem.merchantId.eq(merchantId)
                        .and(qSalesOutboundItem.accountBookId.eq(accountBookId))
                        .and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qSalesOutbound.outboundDate.goe(startDate)))
                .groupBy(qSalesOutboundItem.productId)
                .fetch();

        Map<Long, BigDecimal> salesMap = new HashMap<>();
        for (Tuple t : salesTuples) {
            salesMap.put(t.get(qSalesOutboundItem.productId), nz(t.get(qSalesOutboundItem.quantity.sum())));
        }
        Map<Long, BigDecimal> onOrderMap = onOrderMap(merchantId, accountBookId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : currentTuples) {
            Long productId = t.get(qProduct.id);
            BigDecimal recentSales = salesMap.getOrDefault(productId, BigDecimal.ZERO);
            int current = intAt(t, 7);
            BigDecimal onOrder = onOrderMap.getOrDefault(productId, BigDecimal.ZERO);
            int suggested = ceilInt(recentSales.subtract(BigDecimal.valueOf(current)).subtract(onOrder));

            Map<String, Object> m = new HashMap<>();
            m.put("productId", productId);
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("brand", t.get(qProduct.brand));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("recentSalesQuantity", recentSales);
            m.put("currentQuantity", current);
            m.put("onOrderQuantity", onOrder);
            m.put("alertQuantity", t.get(qProduct.alertQuantity));
            m.put("maxStockQuantity", t.get(qProduct.maxStockQuantity));
            m.put("suggestedQuantity", suggested);
            result.add(m);
        }
        result.sort((a, b) -> Integer.compare((Integer) b.get("suggestedQuantity"), (Integer) a.get("suggestedQuantity")));
        return result;
    }

    /**
     * 智能补货：当前库存 + 在途 < 安全库存(alertQuantity) 时，建议补货至库存上限(maxStockQuantity，缺省用安全库存)。
     */
    public List<Map<String, Object>> replenishment(Long merchantId, Long accountBookId) {
        List<Tuple> currentTuples = jqf.select(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProduct.brand, qProductCategory.name, qUnit.name, qInventory.currentQuantity.sum(),
                        qProduct.alertQuantity, qProduct.maxStockQuantity)
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .where(qInventory.merchantId.eq(merchantId).and(qInventory.accountBookId.eq(accountBookId))
                        .and(qProduct.alertQuantity.isNotNull()))
                .groupBy(qProduct.id, qProduct.code, qProduct.name, qProduct.specification, qProduct.brand,
                        qProductCategory.name, qUnit.name, qProduct.alertQuantity, qProduct.maxStockQuantity)
                .fetch();

        Map<Long, BigDecimal> onOrderMap = onOrderMap(merchantId, accountBookId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : currentTuples) {
            Long productId = t.get(qProduct.id);
            int current = intAt(t, 7);
            int alert = t.get(qProduct.alertQuantity) == null ? 0 : t.get(qProduct.alertQuantity);
            int max = t.get(qProduct.maxStockQuantity) == null ? 0 : t.get(qProduct.maxStockQuantity);
            BigDecimal onOrder = onOrderMap.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal available = BigDecimal.valueOf(current).add(onOrder);

            // 可用库存低于安全库存才建议补货
            if (available.compareTo(BigDecimal.valueOf(alert)) >= 0) {
                continue;
            }
            int target = max > 0 ? max : alert;
            int suggested = ceilInt(BigDecimal.valueOf(target).subtract(available));

            Map<String, Object> m = new HashMap<>();
            m.put("productId", productId);
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("brand", t.get(qProduct.brand));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("currentQuantity", current);
            m.put("onOrderQuantity", onOrder);
            m.put("alertQuantity", alert);
            m.put("maxStockQuantity", max);
            m.put("suggestedQuantity", suggested);
            result.add(m);
        }
        result.sort((a, b) -> Integer.compare((Integer) b.get("suggestedQuantity"), (Integer) a.get("suggestedQuantity")));
        return result;
    }

    /**
     * 一键生成采购订单草稿（已保存状态），返回单据编号。
     */
    @Transactional
    public String generatePurchaseOrder(Long merchantId, Long accountBookId, Long adminId,
                                        Long supplierId, Long warehouseId, List<Item> items) {
        if (supplierId == null) {
            throw new ServiceException("请选择供应商");
        }
        if (items == null || items.isEmpty()) {
            throw new ServiceException("请至少选择一件商品");
        }

        // 过滤无效商品并加载商品信息
        List<Long> productIds = items.stream().map(Item::getProductId).filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : jqf.selectFrom(qProduct)
                .where(qProduct.id.in(productIds).and(qProduct.merchantId.eq(merchantId)))
                .fetch().stream().collect(Collectors.toMap(Product::getId, p -> p));

        // 仓库缺省用系统默认仓
        if (warehouseId == null) {
            Warehouse w = jqf.selectFrom(qWarehouse)
                    .where(qWarehouse.merchantId.eq(merchantId).and(qWarehouse.systemDefault.isTrue()))
                    .fetchFirst();
            if (w != null) {
                warehouseId = w.getId();
            }
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal secondarySum = BigDecimal.ZERO;
        List<PurchaseOrderItem> orderItems = new ArrayList<>();
        for (Item item : items) {
            if (item.getProductId() == null || item.getQuantity() == null || item.getQuantity().signum() <= 0) {
                continue;
            }
            Product product = productMap.get(item.getProductId());
            if (product == null) {
                continue;
            }
            BigDecimal qty = item.getQuantity();
            BigDecimal price = product.getPurchasePrice() == null ? BigDecimal.ZERO : product.getPurchasePrice();

            PurchaseOrderItem pi = new PurchaseOrderItem();
            pi.setProductId(product.getId());
            pi.setBaseUnitId(product.getUnitId());
            pi.setQuantity(qty);
            pi.setSecondaryQuantity(qty);
            pi.setSecondaryUnitId(product.getUnitId());
            pi.setConversionRate(BigDecimal.ONE);
            pi.setUnitPrice(price);
            pi.setSecondaryPrice(price);
            pi.setDiscountRate(BigDecimal.ZERO);
            pi.setDiscountAmount(BigDecimal.ZERO);
            pi.setSubtotal(qty.multiply(price).setScale(2, RoundingMode.HALF_UP));
            pi.setWarehouseId(warehouseId);
            pi.setCreatedBy(adminId);
            pi.setCreatedAt(LocalDateTime.now());
            pi.setMerchantId(merchantId);
            pi.setAccountBookId(accountBookId);
            orderItems.add(pi);

            totalAmount = totalAmount.add(pi.getSubtotal());
            secondarySum = secondarySum.add(qty);
        }
        if (orderItems.isEmpty()) {
            throw new ServiceException("没有可生成的采购明细");
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "采购订单"));
        order.setSupplierId(supplierId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(totalAmount);
        order.setDiscountRate(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(totalAmount);
        order.setSecondarySum(secondarySum);
        order.setRemarks("由采购建议一键生成");
        order.setOrderStatus(OrderStatus.已保存);
        order.setCreatedBy(adminId);
        order.setCreatedAt(LocalDateTime.now());
        order.setMerchantId(merchantId);
        order.setAccountBookId(accountBookId);
        PurchaseOrder saved = purchaseOrderRepository.save(order);

        for (PurchaseOrderItem pi : orderItems) {
            pi.setPurchaseOrderId(saved.getId());
            purchaseOrderItemRepository.save(pi);
        }
        return saved.getOrderNo();
    }

    @Data
    public static class Item {
        private Long productId;
        private BigDecimal quantity;
    }
}
