package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseInboundItem;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrderItem;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOrderItem;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.QSalesOutboundItem;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.OutboundType;
import com.flyemu.share.enums.InboundType;
import com.flyemu.share.repository.purchase.PurchaseInboundItemRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryReportService extends BaseService {

    private final static QInventory qInventory = QInventory.inventory;
    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final static QUnit qUnit = QUnit.unit;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QInventoryItem qInventoryItem = QInventoryItem.inventoryItem;
    private final static QInventoryTransfer qInventoryTransfer = QInventoryTransfer.inventoryTransfer;
    private final static QInventoryTransferItem qInventoryTransferItem = QInventoryTransferItem.inventoryTransferItem;
    private final static QOtherOutbound qOtherOutbound = QOtherOutbound.otherOutbound;
    private final static QOtherOutboundItem qOtherOutboundItem = QOtherOutboundItem.otherOutboundItem;
    private final static QOtherInbound qOtherInbound = QOtherInbound.otherInbound;
    private final static QOtherInboundItem qOtherInboundItem = QOtherInboundItem.otherInboundItem;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;
    private final static QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;

    private final PurchaseInboundItemRepository purchaseInboundItemRepository;

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 安全读取聚合列（Integer 列的 SUM/MAX 在 MySQL 下可能返回 DECIMAL/Long，
     * 直接 cast 成 Integer 会抛 ClassCastException，故统一按 Number 读取）。
     */
    private static int intAt(Tuple t, int index) {
        Number n = t.get(index, Number.class);
        return n == null ? 0 : n.intValue();
    }

    private static BigDecimal decAt(Tuple t, int index) {
        Number n = t.get(index, Number.class);
        return n == null ? BigDecimal.ZERO : new BigDecimal(n.toString());
    }

    /**
     * 库存预警：当前库存 <= 预警库存 的商品
     */
    public List<Map<String, Object>> warning(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qProduct.id, qProduct.code, qProduct.name, qProduct.specification, qProduct.brand,
                        qProductCategory.name, qUnit.name, qWarehouse.name,
                        qInventory.currentQuantity, qProduct.alertQuantity)
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventory.warehouseId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qProduct.alertQuantity.isNotNull())
                        .and(qInventory.currentQuantity.loe(qProduct.alertQuantity)))
                .orderBy(qProduct.id.asc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", t.get(qProduct.id));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("brand", t.get(qProduct.brand));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("warehouseName", t.get(qWarehouse.name));
            Integer current = t.get(qInventory.currentQuantity);
            Integer alert = t.get(qProduct.alertQuantity);
            int cur = current == null ? 0 : current;
            int al = alert == null ? 0 : alert;
            m.put("currentQuantity", cur);
            m.put("alertQuantity", al);
            m.put("shortageQuantity", al - cur);
            result.add(m);
        }
        return result;
    }

    /**
     * 库存上限预警：当前库存 >= 库存上限 的商品（超储）
     */
    public List<Map<String, Object>> overstockWarning(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qProduct.id, qProduct.code, qProduct.name, qProduct.specification, qProduct.brand,
                        qProductCategory.name, qUnit.name, qWarehouse.name,
                        qInventory.currentQuantity, qProduct.maxStockQuantity)
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventory.warehouseId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qProduct.maxStockQuantity.isNotNull())
                        .and(qInventory.currentQuantity.goe(qProduct.maxStockQuantity)))
                .orderBy(qProduct.id.asc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", t.get(qProduct.id));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("brand", t.get(qProduct.brand));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("warehouseName", t.get(qWarehouse.name));
            Integer current = t.get(qInventory.currentQuantity);
            Integer max = t.get(qProduct.maxStockQuantity);
            int cur = current == null ? 0 : current;
            int mx = max == null ? 0 : max;
            m.put("currentQuantity", cur);
            m.put("maxStockQuantity", mx);
            m.put("excessQuantity", cur - mx);
            result.add(m);
        }
        return result;
    }

    /**
     * 首页预警汇总：缺货/超储/保质期预警数量
     */
    public Map<String, Object> warningSummary(Long merchantId, Long accountBookId) {
        // 缺货（低于预警库存）
        Long shortageCount = jqf.select(qInventory.count())
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qProduct.alertQuantity.isNotNull())
                        .and(qInventory.currentQuantity.loe(qProduct.alertQuantity)))
                .fetchOne();

        // 超储（达到/超过库存上限）
        Long overstockCount = jqf.select(qInventory.count())
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qProduct.maxStockQuantity.isNotNull())
                        .and(qInventory.currentQuantity.goe(qProduct.maxStockQuantity)))
                .fetchOne();

        // 保质期预警（即将到期或已过期）
        LocalDate today = LocalDate.now();
        LocalDate soonThreshold = today.plusDays(30);
        Long expiryCount = jqf.select(qPurchaseInboundItem.count())
                .from(qPurchaseInboundItem)
                .where(qPurchaseInboundItem.merchantId.eq(merchantId)
                        .and(qPurchaseInboundItem.accountBookId.eq(accountBookId))
                        .and(qPurchaseInboundItem.expiryDate.isNotNull())
                        .and(qPurchaseInboundItem.expiryDate.loe(soonThreshold)))
                .fetchOne();

        Map<String, Object> m = new HashMap<>();
        m.put("shortageCount", shortageCount == null ? 0 : shortageCount);
        m.put("overstockCount", overstockCount == null ? 0 : overstockCount);
        m.put("expiryCount", expiryCount == null ? 0 : expiryCount);
        return m;
    }

    /**
     * 库存状况总览：按商品汇总（跨仓库）
     * select: 0 id,1 code,2 name,3 spec,4 brand,5 catName,6 unitName,7 qtySum,8 costSum
     */
    public List<Map<String, Object>> overview(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qProduct.id, qProduct.code, qProduct.name, qProduct.specification, qProduct.brand,
                        qProductCategory.name, qUnit.name,
                        qInventory.currentQuantity.sum(), qInventory.totalCost.sum())
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId)))
                .groupBy(qProduct.id, qProduct.code, qProduct.name, qProduct.specification, qProduct.brand,
                        qProductCategory.name, qUnit.name)
                .orderBy(qProduct.id.desc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            int quantity = intAt(t, 7);
            BigDecimal totalCost = decAt(t, 8).setScale(2, RoundingMode.HALF_UP);
            BigDecimal avgCost = quantity > 0 ? totalCost.divide(BigDecimal.valueOf(quantity), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            Map<String, Object> m = new HashMap<>();
            m.put("productId", t.get(qProduct.id));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("brand", t.get(qProduct.brand));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("quantity", quantity);
            m.put("totalCost", totalCost);
            m.put("averageCost", avgCost);
            result.add(m);
        }
        return result;
    }

    /**
     * 库存分布：按仓库汇总
     * select: 0 warehouseId,1 code,2 name,3 qtySum,4 costSum,5 productCount
     */
    public List<Map<String, Object>> distribution(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qWarehouse.id, qWarehouse.code, qWarehouse.name,
                        qInventory.currentQuantity.sum(), qInventory.totalCost.sum(),
                        qProduct.id.countDistinct())
                .from(qInventory)
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventory.warehouseId))
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId)))
                .groupBy(qWarehouse.id, qWarehouse.code, qWarehouse.name)
                .orderBy(qWarehouse.id.asc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            Map<String, Object> m = new HashMap<>();
            m.put("warehouseId", t.get(qWarehouse.id));
            m.put("warehouseCode", t.get(qWarehouse.code));
            m.put("warehouseName", t.get(qWarehouse.name));
            m.put("quantity", intAt(t, 3));
            m.put("totalCost", decAt(t, 4).setScale(2, RoundingMode.HALF_UP));
            m.put("productCount", t.get(qProduct.id.countDistinct()));
            result.add(m);
        }
        return result;
    }

    /**
     * 虚拟库存：账面库存 + 采购在途 - 销售占用
     */
    public List<Map<String, Object>> virtualStock(Long merchantId, Long accountBookId) {
        // 账面库存（按商品汇总）：0 id,1 code,2 name,3 spec,4 catName,5 unitName,6 qtySum,7 costSum
        List<Tuple> currentTuples = jqf.select(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProductCategory.name, qUnit.name, qInventory.currentQuantity.sum(), qInventory.totalCost.sum())
                .from(qInventory)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .where(qInventory.merchantId.eq(merchantId).and(qInventory.accountBookId.eq(accountBookId)))
                .groupBy(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProductCategory.name, qUnit.name)
                .fetch();

        // 采购在途：已审核且未生成入库单的采购订单数量
        List<Tuple> onOrderTuples = jqf.select(qPurchaseOrderItem.productId, qPurchaseOrderItem.quantity.sum())
                .from(qPurchaseOrderItem)
                .leftJoin(qPurchaseOrder).on(qPurchaseOrder.id.eq(qPurchaseOrderItem.purchaseOrderId))
                .where(qPurchaseOrderItem.merchantId.eq(merchantId)
                        .and(qPurchaseOrderItem.accountBookId.eq(accountBookId))
                        .and(qPurchaseOrder.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseOrder.purchaseInboundId.isNull()))
                .groupBy(qPurchaseOrderItem.productId)
                .fetch();

        // 销售占用：已审核且未全部出库的销售订单数量
        List<Tuple> reservedTuples = jqf.select(qSalesOrderItem.productId, qSalesOrderItem.quantity.sum(), qSalesOrderItem.quantityOut.sum())
                .from(qSalesOrderItem)
                .leftJoin(qSalesOrder).on(qSalesOrder.id.eq(qSalesOrderItem.salesOrderId))
                .where(qSalesOrderItem.merchantId.eq(merchantId)
                        .and(qSalesOrderItem.accountBookId.eq(accountBookId))
                        .and(qSalesOrder.orderStatus.eq(OrderStatus.已审核))
                        .and(qSalesOrder.status.lt(2)))
                .groupBy(qSalesOrderItem.productId)
                .fetch();

        Map<Long, BigDecimal> onOrderMap = new HashMap<>();
        for (Tuple t : onOrderTuples) {
            onOrderMap.put(t.get(qPurchaseOrderItem.productId), nz(t.get(qPurchaseOrderItem.quantity.sum())));
        }
        Map<Long, BigDecimal> reservedMap = new HashMap<>();
        for (Tuple t : reservedTuples) {
            BigDecimal qty = nz(t.get(qSalesOrderItem.quantity.sum()));
            BigDecimal out = nz(t.get(qSalesOrderItem.quantityOut.sum()));
            reservedMap.put(t.get(qSalesOrderItem.productId), qty.subtract(out));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : currentTuples) {
            Long productId = t.get(qProduct.id);
            BigDecimal current = BigDecimal.valueOf(intAt(t, 6));
            BigDecimal onOrder = onOrderMap.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal reserved = reservedMap.getOrDefault(productId, BigDecimal.ZERO);
            BigDecimal available = current.add(onOrder).subtract(reserved);
            BigDecimal totalCost = decAt(t, 7).setScale(2, RoundingMode.HALF_UP);

            Map<String, Object> m = new HashMap<>();
            m.put("productId", productId);
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("currentQuantity", current);
            m.put("onOrderQuantity", onOrder);
            m.put("reservedQuantity", reserved);
            m.put("availableQuantity", available);
            m.put("totalCost", totalCost);
            result.add(m);
        }
        return result;
    }

    /**
     * 调拨统计：按商品汇总调拨数量（已审核调拨单）
     * select: 0 id,1 code,2 name,3 spec,4 catName,5 unitName,6 qtySum
     */
    public List<Map<String, Object>> transferReport(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProductCategory.name, qUnit.name, qInventoryTransferItem.quantity.sum())
                .from(qInventoryTransferItem)
                .leftJoin(qInventoryTransfer).on(qInventoryTransfer.id.eq(qInventoryTransferItem.inventoryTransferId))
                .leftJoin(qProduct).on(qProduct.id.eq(qInventoryTransferItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .where(qInventoryTransferItem.merchantId.eq(merchantId)
                        .and(qInventoryTransferItem.accountBookId.eq(accountBookId))
                        .and(qInventoryTransfer.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProductCategory.name, qUnit.name)
                .orderBy(qProduct.id.desc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", t.get(qProduct.id));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("quantity", nz(t.get(qInventoryTransferItem.quantity.sum())));
            result.add(m);
        }
        return result;
    }

    /**
     * 批次跟踪：按商品+批次汇总库存明细
     * select: 0 id,1 code,2 name,3 spec,4 catName,5 unitName,6 batchNumber,7 qtySum,8 curQtyMax
     */
    public List<Map<String, Object>> batchReport(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProductCategory.name, qUnit.name, qInventoryItem.batchNumber,
                        qInventoryItem.quantity.sum(), qInventoryItem.currentQuantity.max())
                .from(qInventoryItem)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventoryItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .where(qInventoryItem.merchantId.eq(merchantId)
                        .and(qInventoryItem.accountBookId.eq(accountBookId))
                        .and(qInventoryItem.batchNumber.isNotNull())
                        .and(qInventoryItem.batchNumber.ne("")))
                .groupBy(qProduct.id, qProduct.code, qProduct.name, qProduct.specification,
                        qProductCategory.name, qUnit.name, qInventoryItem.batchNumber)
                .orderBy(qProduct.id.desc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            Map<String, Object> m = new HashMap<>();
            m.put("productId", t.get(qProduct.id));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("productCategoryName", t.get(qProductCategory.name));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("batchNumber", t.get(qInventoryItem.batchNumber));
            m.put("quantity", intAt(t, 7));
            m.put("currentQuantity", intAt(t, 8));
            result.add(m);
        }
        return result;
    }

    /**
     * 报损报溢汇总：按商品汇总盘亏/盘盈数量与金额
     */
    public List<Map<String, Object>> lossGainReport(Long merchantId, Long accountBookId) {
        // 报损（盘亏出库）
        List<Tuple> lossTuples = jqf.select(qOtherOutboundItem.productId,
                        qOtherOutboundItem.quantity.sum(), qOtherOutboundItem.costAmount.sum())
                .from(qOtherOutboundItem)
                .leftJoin(qOtherOutbound).on(qOtherOutbound.id.eq(qOtherOutboundItem.otherOutboundId))
                .where(qOtherOutboundItem.merchantId.eq(merchantId)
                        .and(qOtherOutboundItem.accountBookId.eq(accountBookId))
                        .and(qOtherOutbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qOtherOutbound.outboundType.eq(OutboundType.盘亏出库)))
                .groupBy(qOtherOutboundItem.productId)
                .fetch();

        // 报溢（盘盈入库）
        List<Tuple> gainTuples = jqf.select(qOtherInboundItem.productId,
                        qOtherInboundItem.quantity.sum(), qOtherInboundItem.subtotal.sum())
                .from(qOtherInboundItem)
                .leftJoin(qOtherInbound).on(qOtherInbound.id.eq(qOtherInboundItem.otherInboundId))
                .where(qOtherInboundItem.merchantId.eq(merchantId)
                        .and(qOtherInboundItem.accountBookId.eq(accountBookId))
                        .and(qOtherInbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qOtherInbound.inboundType.eq(InboundType.盘盈入库)))
                .groupBy(qOtherInboundItem.productId)
                .fetch();

        Set<Long> productIds = new HashSet<>();
        Map<Long, BigDecimal[]> lossMap = new HashMap<>();
        for (Tuple t : lossTuples) {
            Long pid = t.get(qOtherOutboundItem.productId);
            productIds.add(pid);
            lossMap.put(pid, new BigDecimal[]{nz(t.get(qOtherOutboundItem.quantity.sum())), nz(t.get(qOtherOutboundItem.costAmount.sum()))});
        }
        Map<Long, BigDecimal[]> gainMap = new HashMap<>();
        for (Tuple t : gainTuples) {
            Long pid = t.get(qOtherInboundItem.productId);
            productIds.add(pid);
            gainMap.put(pid, new BigDecimal[]{nz(t.get(qOtherInboundItem.quantity.sum())), nz(t.get(qOtherInboundItem.subtotal.sum()))});
        }

        // 批量取商品信息，避免 N+1
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : jqf.selectFrom(qProduct).where(qProduct.id.in(productIds)).fetch()
                .stream().collect(Collectors.toMap(Product::getId, p -> p));

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long pid : productIds) {
            Product product = productMap.get(pid);
            BigDecimal[] loss = lossMap.getOrDefault(pid, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] gain = gainMap.getOrDefault(pid, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            Map<String, Object> m = new HashMap<>();
            m.put("productId", pid);
            m.put("productCode", product != null ? product.getCode() : "");
            m.put("productName", product != null ? product.getName() : "");
            m.put("productSpecification", product != null ? product.getSpecification() : "");
            m.put("lossQuantity", loss[0]);
            m.put("lossAmount", loss[1].setScale(2, RoundingMode.HALF_UP));
            m.put("gainQuantity", gain[0]);
            m.put("gainAmount", gain[1].setScale(2, RoundingMode.HALF_UP));
            m.put("netQuantity", gain[0].subtract(loss[0]));
            m.put("netAmount", gain[1].subtract(loss[1]).setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        }
        return result;
    }

    /**
     * 保质期预警：列出采购入库明细中已登记有效期至的商品批次，并计算剩余天数/是否过期。
     */
    public List<Map<String, Object>> expiryWarning(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qPurchaseInboundItem.id, qPurchaseInboundItem.productId, qPurchaseInboundItem.batchNumber,
                        qPurchaseInboundItem.productionDate, qPurchaseInboundItem.expiryDate,
                        qPurchaseInboundItem.quantity, qPurchaseInboundItem.warehouseId,
                        qProduct.code, qProduct.name, qProduct.specification, qUnit.name, qWarehouse.name)
                .from(qPurchaseInboundItem)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId))
                .where(qPurchaseInboundItem.merchantId.eq(merchantId)
                        .and(qPurchaseInboundItem.accountBookId.eq(accountBookId))
                        .and(qPurchaseInboundItem.expiryDate.isNotNull()))
                .orderBy(qPurchaseInboundItem.expiryDate.asc())
                .fetch();

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            LocalDate expiry = t.get(qPurchaseInboundItem.expiryDate);
            long days = ChronoUnit.DAYS.between(today, expiry);
            Map<String, Object> m = new HashMap<>();
            m.put("itemId", t.get(qPurchaseInboundItem.id));
            m.put("productId", t.get(qPurchaseInboundItem.productId));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("warehouseName", t.get(qWarehouse.name));
            m.put("batchNumber", t.get(qPurchaseInboundItem.batchNumber));
            m.put("productionDate", t.get(qPurchaseInboundItem.productionDate));
            m.put("expiryDate", expiry);
            m.put("quantity", t.get(qPurchaseInboundItem.quantity));
            m.put("daysToExpiry", days);
            m.put("expired", days < 0);
            m.put("expiringSoon", days >= 0 && days <= 30);
            m.put("expiryStatus", days < 0 ? "已过期" : (days <= 30 ? "即将到期" : "正常"));
            result.add(m);
        }
        return result;
    }

    /**
     * 保质期管理列表：列出全部采购入库明细（含未登记效期的），补齐商品/仓库名称与剩余天数。
     */
    public List<Map<String, Object>> shelfLifeList(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.select(
                        qPurchaseInboundItem.id, qPurchaseInboundItem.productId, qPurchaseInboundItem.batchNumber,
                        qPurchaseInboundItem.productionDate, qPurchaseInboundItem.expiryDate,
                        qPurchaseInboundItem.quantity, qPurchaseInboundItem.warehouseId,
                        qProduct.code, qProduct.name, qProduct.specification, qUnit.name, qWarehouse.name)
                .from(qPurchaseInboundItem)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId))
                .where(qPurchaseInboundItem.merchantId.eq(merchantId)
                        .and(qPurchaseInboundItem.accountBookId.eq(accountBookId)))
                .orderBy(qPurchaseInboundItem.id.desc())
                .fetch();

        LocalDate today = LocalDate.now();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : tuples) {
            LocalDate expiry = t.get(qPurchaseInboundItem.expiryDate);
            Long days = expiry == null ? null : ChronoUnit.DAYS.between(today, expiry);
            Map<String, Object> m = new HashMap<>();
            m.put("itemId", t.get(qPurchaseInboundItem.id));
            m.put("productId", t.get(qPurchaseInboundItem.productId));
            m.put("productCode", t.get(qProduct.code));
            m.put("productName", t.get(qProduct.name));
            m.put("productSpecification", t.get(qProduct.specification));
            m.put("productUnitName", t.get(qUnit.name));
            m.put("warehouseName", t.get(qWarehouse.name));
            m.put("batchNumber", t.get(qPurchaseInboundItem.batchNumber));
            m.put("productionDate", t.get(qPurchaseInboundItem.productionDate));
            m.put("expiryDate", expiry);
            m.put("quantity", t.get(qPurchaseInboundItem.quantity));
            m.put("daysToExpiry", days);
            m.put("expiryStatus", expiry == null ? "未登记" : (days < 0 ? "已过期" : (days <= 30 ? "即将到期" : "正常")));
            result.add(m);
        }
        return result;
    }

    /**
     * 维护采购入库明细的批次号/生产日期/有效期至。
     */
    @Transactional
    public void updateShelfLife(Long merchantId, Long accountBookId, Long itemId, String batchNumber,
                                LocalDate productionDate, LocalDate expiryDate) {
        var item = purchaseInboundItemRepository.findById(itemId)
                .filter(x -> merchantId.equals(x.getMerchantId()))
                .orElseThrow(() -> new RuntimeException("入库明细不存在: id=" + itemId));
        item.setBatchNumber(batchNumber);
        item.setProductionDate(productionDate);
        item.setExpiryDate(expiryDate);
        purchaseInboundItemRepository.save(item);
    }

    /**
     * 批次出库可选批次：按商品+仓库列出采购入库登记的批次号，并计算剩余可用数量
     * （已审核入库量 − 已审核出库量），按有效期先后排序（先到先出提示）。
     */
    public List<Map<String, Object>> batchAvailable(Long merchantId, Long accountBookId, Long productId, Long warehouseId) {
        List<Tuple> inTuples = jqf.select(qPurchaseInboundItem.batchNumber,
                        qPurchaseInboundItem.productionDate.min(), qPurchaseInboundItem.expiryDate.min(),
                        qPurchaseInboundItem.quantity.sum())
                .from(qPurchaseInboundItem)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .where(qPurchaseInboundItem.merchantId.eq(merchantId)
                        .and(qPurchaseInboundItem.accountBookId.eq(accountBookId))
                        .and(qPurchaseInboundItem.productId.eq(productId))
                        .and(qPurchaseInboundItem.warehouseId.eq(warehouseId))
                        .and(qPurchaseInboundItem.batchNumber.isNotNull())
                        .and(qPurchaseInboundItem.batchNumber.ne(""))
                        .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(qPurchaseInboundItem.batchNumber)
                .fetch();

        List<Tuple> outTuples = jqf.select(qSalesOutboundItem.batchNumber, qSalesOutboundItem.quantity.sum())
                .from(qSalesOutboundItem)
                .leftJoin(qSalesOutbound).on(qSalesOutbound.id.eq(qSalesOutboundItem.salesOutboundId))
                .where(qSalesOutboundItem.merchantId.eq(merchantId)
                        .and(qSalesOutboundItem.accountBookId.eq(accountBookId))
                        .and(qSalesOutboundItem.productId.eq(productId))
                        .and(qSalesOutboundItem.warehouseId.eq(warehouseId))
                        .and(qSalesOutboundItem.batchNumber.isNotNull())
                        .and(qSalesOutboundItem.batchNumber.ne(""))
                        .and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(qSalesOutboundItem.batchNumber)
                .fetch();

        Map<String, BigDecimal> outMap = new HashMap<>();
        for (Tuple t : outTuples) {
            outMap.put(t.get(qSalesOutboundItem.batchNumber), nz(t.get(qSalesOutboundItem.quantity.sum())));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple t : inTuples) {
            String batchNumber = t.get(qPurchaseInboundItem.batchNumber);
            BigDecimal inQty = nz(t.get(qPurchaseInboundItem.quantity.sum()));
            BigDecimal avail = inQty.subtract(outMap.getOrDefault(batchNumber, BigDecimal.ZERO));
            if (avail.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> m = new HashMap<>();
            m.put("batchNumber", batchNumber);
            m.put("productionDate", t.get(qPurchaseInboundItem.productionDate.min()));
            m.put("expiryDate", t.get(qPurchaseInboundItem.expiryDate.min()));
            m.put("availableQuantity", avail.setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        }
        result.sort(Comparator.comparing(
                m -> (LocalDate) m.get("expiryDate"),
                Comparator.nullsLast(Comparator.naturalOrder())));
        return result;
    }
}
