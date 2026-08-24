package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.inventory.AssemblyOrderDto;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.AssemblyOrder;
import com.flyemu.share.entity.inventory.AssemblyOrderItem;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.inventory.QAssemblyOrder;
import com.flyemu.share.entity.inventory.QAssemblyOrderItem;
import com.flyemu.share.enums.AssemblyOrderType;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.AssemblyOrderForm;
import com.flyemu.share.repository.inventory.AssemblyOrderItemRepository;
import com.flyemu.share.repository.inventory.AssemblyOrderRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组装拆卸单：审核时联动库存与成本。
 * 组装 = 组件出库 + 成品入库；拆卸 = 成品出库 + 组件入库。
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssemblyOrderService extends BaseService {

    private final static QAssemblyOrder qAssemblyOrder = QAssemblyOrder.assemblyOrder;
    private final static QAssemblyOrderItem qAssemblyOrderItem = QAssemblyOrderItem.assemblyOrderItem;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final AssemblyOrderRepository assemblyOrderRepository;
    private final AssemblyOrderItemRepository assemblyOrderItemRepository;
    private final InventoryService inventoryService;
    private final CostingService costingService;
    private final CodeSeedService codeSeedService;

    public PageResults<AssemblyOrderDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qAssemblyOrder)
                .select(qAssemblyOrder, qProduct.code, qProduct.name, qProduct.specification,
                        qUnit.name, qWarehouse.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qAssemblyOrder.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qAssemblyOrder.warehouseId))
                .where(query.builder)
                .orderBy(qAssemblyOrder.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<AssemblyOrderDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AssemblyOrderDto dto = BeanUtil.toBean(tuple.get(qAssemblyOrder), AssemblyOrderDto.class);
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dto.setProductSpecification(tuple.get(qProduct.specification));
            dto.setProductUnitName(tuple.get(qUnit.name));
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dtos.add(dto);
        });
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public AssemblyOrder save(AssemblyOrderForm form, Long merchantId) {
        AssemblyOrder order = form.getAssemblyOrder();
        Assert.notNull(order, "组装拆卸单不能为空");
        Assert.notNull(order.getProductId(), "请选择成品/被拆品");
        Assert.notNull(order.getWarehouseId(), "请选择仓库");
        Assert.isTrue(order.getQuantity() != null && order.getQuantity().compareTo(BigDecimal.ZERO) > 0, "数量必须大于0");
        Assert.isTrue(CollUtil.isNotEmpty(form.getAssemblyOrderItemList()), "请至少添加一条组件明细");

        if (order.getId() != null) {
            AssemblyOrder original = assemblyOrderRepository.getById(order.getId());
            Assert.isFalse(OrderStatus.已审核.equals(original.getOrderStatus()), "已审核单据不能修改~");
            BeanUtil.copyProperties(order, original, CopyOptions.create().ignoreNullValue());
            // 明细整表重建
            jqf.delete(qAssemblyOrderItem)
                    .where(qAssemblyOrderItem.assemblyOrderId.eq(original.getId()))
                    .execute();
            assemblyOrderRepository.save(original);
            saveItems(original, form.getAssemblyOrderItemList(), merchantId);
            return original;
        }
        order.setId(null);
        order.setOrderNo(codeSeedService.generateCode(order.getMerchantId(), order.getAccountBookId(), "组装拆卸单"));
        order.setOrderStatus(OrderStatus.已保存);
        order.setCreatedAt(LocalDateTime.now());
        assemblyOrderRepository.save(order);
        saveItems(order, form.getAssemblyOrderItemList(), merchantId);
        return order;
    }

    private void saveItems(AssemblyOrder order, List<AssemblyOrderItem> items, Long merchantId) {
        for (AssemblyOrderItem item : items) {
            item.setId(null);
            item.setAssemblyOrderId(order.getId());
            item.setMerchantId(order.getMerchantId());
            item.setAccountBookId(order.getAccountBookId());
        }
        assemblyOrderItemRepository.saveAll(items);
    }

    public Dict load(Long merchantId, Long orderId) {
        AssemblyOrder order = bqf.selectFrom(qAssemblyOrder)
                .where(qAssemblyOrder.id.eq(orderId).and(qAssemblyOrder.merchantId.eq(merchantId)))
                .fetchFirst();
        Assert.notNull(order, "单据不存在");
        AssemblyOrderDto dto = BeanUtil.toBean(order, AssemblyOrderDto.class);
        List<AssemblyOrderItem> items = jqf.selectFrom(qAssemblyOrderItem)
                .where(qAssemblyOrderItem.assemblyOrderId.eq(orderId))
                .orderBy(qAssemblyOrderItem.id.asc())
                .fetch();

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (AssemblyOrderItem item : items) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("productId", item.getProductId());
            m.put("quantity", item.getQuantity());
            m.put("costAmount", item.getCostAmount());
            Product p = jqf.selectFrom(qProduct).where(qProduct.id.eq(item.getProductId())).fetchOne();
            if (p != null) {
                m.put("productCode", p.getCode());
                m.put("productName", p.getName());
                m.put("productSpecification", p.getSpecification());
                m.put("unitId", p.getUnitId());
                String unitName = jqf.select(qUnit.name).from(qUnit).where(qUnit.id.eq(p.getUnitId())).fetchOne();
                m.put("unitName", unitName != null ? unitName : "");
            }
            itemMaps.add(m);
        }
        return Dict.create().set("assemblyOrder", dto).set("assemblyOrderItemList", itemMaps);
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        List<AssemblyOrder> orders = bqf.selectFrom(qAssemblyOrder)
                .where(qAssemblyOrder.merchantId.eq(merchantId).and(qAssemblyOrder.id.in(ids)))
                .fetch();
        Assert.isFalse(CollUtil.isEmpty(orders), "未找到数据~");
        for (AssemblyOrder order : orders) {
            if (OrderStatus.已审核.equals(state)) {
                if (OrderStatus.已审核.equals(order.getOrderStatus())) {
                    continue;
                }
                auditStock(order);
                order.setOrderStatus(OrderStatus.已审核);
                order.setApprovedBy(adminId);
                order.setApprovedAt(LocalDateTime.now());
                assemblyOrderRepository.save(order);
            } else if (OrderStatus.已保存.equals(state)) {
                if (!OrderStatus.已审核.equals(order.getOrderStatus())) {
                    continue;
                }
                reverseStock(order);
                order.setOrderStatus(OrderStatus.已保存);
                order.setApprovedBy(null);
                order.setApprovedAt(null);
                assemblyOrderRepository.save(order);
            }
        }
    }

    /**
     * 审核：按类型联动库存与成本。
     */
    private void auditStock(AssemblyOrder order) {
        List<AssemblyOrderItem> items = jqf.selectFrom(qAssemblyOrderItem)
                .where(qAssemblyOrderItem.assemblyOrderId.eq(order.getId()))
                .fetch();
        boolean assemble = AssemblyOrderType.组装.equals(order.getOrderType());
        OperationType outType = assemble ? OperationType.组装出库 : OperationType.拆卸出库;
        OperationType inType = assemble ? OperationType.组装入库 : OperationType.拆卸入库;
        Long merchantId = order.getMerchantId();
        Long accountBookId = order.getAccountBookId();

        int mainQty = order.getQuantity() == null ? 0 : order.getQuantity().intValue();
        Product mainProduct = findProduct(order.getProductId());

        List<Inventory> outInventories = new ArrayList<>();
        List<InventoryItem> outItems = new ArrayList<>();
        List<Inventory> inInventories = new ArrayList<>();
        List<InventoryItem> inItems = new ArrayList<>();
        Date now = new Date();

        if (assemble) {
            BigDecimal mainCost = BigDecimal.ZERO;
            for (AssemblyOrderItem it : items) {
                int qty = it.getQuantity() == null ? 0 : it.getQuantity().intValue();
                CostingService.IssueRequest req = new CostingService.IssueRequest();
                req.setProductId(it.getProductId());
                req.setWarehouseId(order.getWarehouseId());
                req.setQty(qty);
                req.setOrderId(order.getId());
                req.setOrderType(outType);
                req.setItemId(it.getId());
                req.setMerchantId(merchantId);
                req.setAccountBookId(accountBookId);
                CostingService.IssueResult res = costingService.issue(req);
                BigDecimal cost = res.getCostAmount();
                it.setCostAmount(cost);
                mainCost = mainCost.add(cost);

                Product p = findProduct(it.getProductId());
                outInventories.add(buildInventory(p, order.getWarehouseId(), qty, cost, merchantId, accountBookId));
                outItems.add(buildInventoryItem(p, order.getWarehouseId(), qty, cost, outType, order, now));
            }
            // 成品入库：成本 = 组件出库成本合计
            if (mainQty > 0) {
                BigDecimal unitCost = mainCost.divide(BigDecimal.valueOf(mainQty), 6, RoundingMode.HALF_EVEN);
                createReceiptBatch(mainProduct, order, mainQty, unitCost, inType, null);
                inInventories.add(buildInventory(mainProduct, order.getWarehouseId(), mainQty, mainCost, merchantId, accountBookId));
                inItems.add(buildInventoryItem(mainProduct, order.getWarehouseId(), mainQty, mainCost, inType, order, now));
            }
            order.setCostAmount(mainCost);
        } else {
            // 拆卸：成品出库
            CostingService.IssueRequest req = new CostingService.IssueRequest();
            req.setProductId(order.getProductId());
            req.setWarehouseId(order.getWarehouseId());
            req.setQty(mainQty);
            req.setOrderId(order.getId());
            req.setOrderType(outType);
            req.setItemId(null);
            req.setMerchantId(merchantId);
            req.setAccountBookId(accountBookId);
            CostingService.IssueResult res = costingService.issue(req);
            BigDecimal mainCost = res.getCostAmount();
            order.setCostAmount(mainCost);
            outInventories.add(buildInventory(mainProduct, order.getWarehouseId(), mainQty, mainCost, merchantId, accountBookId));
            outItems.add(buildInventoryItem(mainProduct, order.getWarehouseId(), mainQty, mainCost, outType, order, now));

            // 组件入库：按数量比例分摊成品成本（单位成本 = 成品成本 / 组件总数量）
            int totalQty = items.stream().mapToInt(i -> i.getQuantity() == null ? 0 : i.getQuantity().intValue()).sum();
            BigDecimal unitCost = totalQty > 0
                    ? mainCost.divide(BigDecimal.valueOf(totalQty), 6, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO;
            for (AssemblyOrderItem it : items) {
                int qty = it.getQuantity() == null ? 0 : it.getQuantity().intValue();
                BigDecimal cost = unitCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_EVEN);
                it.setCostAmount(cost);
                Product p = findProduct(it.getProductId());
                createReceiptBatch(p, order, qty, unitCost, inType, it.getId());
                inInventories.add(buildInventory(p, order.getWarehouseId(), qty, cost, merchantId, accountBookId));
                inItems.add(buildInventoryItem(p, order.getWarehouseId(), qty, cost, inType, order, now));
            }
        }
        assemblyOrderItemRepository.saveAll(items);
        assemblyOrderRepository.save(order);

        // 更新库存余额
        outInventories.forEach(inv -> inventoryService.computedInventory(inv, false, order.getId(), outType, outItems));
        inInventories.forEach(inv -> inventoryService.computedInventory(inv, true, order.getId(), inType, inItems));
    }

    /**
     * 反审核：回补库存与成本。
     */
    private void reverseStock(AssemblyOrder order) {
        List<AssemblyOrderItem> items = jqf.selectFrom(qAssemblyOrderItem)
                .where(qAssemblyOrderItem.assemblyOrderId.eq(order.getId()))
                .fetch();
        boolean assemble = AssemblyOrderType.组装.equals(order.getOrderType());
        OperationType outType = assemble ? OperationType.组装出库 : OperationType.拆卸出库;
        OperationType inType = assemble ? OperationType.组装入库 : OperationType.拆卸入库;
        Long merchantId = order.getMerchantId();
        Long accountBookId = order.getAccountBookId();

        // 先反审入库批次，再反审出库耗用
        costingService.reverseReceipt(order.getId(), inType, merchantId, accountBookId);
        costingService.reverseIssue(order.getId(), outType, merchantId, accountBookId);

        int mainQty = order.getQuantity() == null ? 0 : order.getQuantity().intValue();
        BigDecimal mainCost = order.getCostAmount() == null ? BigDecimal.ZERO : order.getCostAmount();
        Product mainProduct = findProduct(order.getProductId());

        // 主品回补：组装时主品入库→减库存；拆卸时主品出库→加库存
        if (assemble) {
            inventoryService.computedInventory(
                    buildInventory(mainProduct, order.getWarehouseId(), mainQty, mainCost, merchantId, accountBookId),
                    false, order.getId(), inType, null);
        } else {
            inventoryService.computedInventory(
                    buildInventory(mainProduct, order.getWarehouseId(), mainQty, mainCost, merchantId, accountBookId),
                    true, order.getId(), outType, null);
        }
        // 组件回补：组装时组件出库→加库存；拆卸时组件入库→减库存
        for (AssemblyOrderItem it : items) {
            int qty = it.getQuantity() == null ? 0 : it.getQuantity().intValue();
            BigDecimal cost = it.getCostAmount() == null ? BigDecimal.ZERO : it.getCostAmount();
            Product p = findProduct(it.getProductId());
            if (assemble) {
                inventoryService.computedInventory(
                        buildInventory(p, order.getWarehouseId(), qty, cost, merchantId, accountBookId),
                        true, order.getId(), outType, null);
            } else {
                inventoryService.computedInventory(
                        buildInventory(p, order.getWarehouseId(), qty, cost, merchantId, accountBookId),
                        false, order.getId(), inType, null);
            }
        }
    }

    private Product findProduct(Long productId) {
        if (productId == null) {
            throw new ServiceException("商品不能为空");
        }
        Product p = jqf.selectFrom(qProduct).where(qProduct.id.eq(productId)).fetchOne();
        if (p == null) {
            throw new ServiceException("商品不存在: id=" + productId);
        }
        return p;
    }

    private Inventory buildInventory(Product p, Long warehouseId, int qty, BigDecimal cost, Long merchantId, Long accountBookId) {
        Inventory inv = new Inventory();
        inv.setProductId(p.getId());
        inv.setWarehouseId(warehouseId);
        inv.setCurrentQuantity(qty);
        inv.setTotalCost(cost);
        inv.setMerchantId(merchantId);
        inv.setAccountBookId(accountBookId);
        inv.setBaseUnitId(p.getUnitId());
        return inv;
    }

    private InventoryItem buildInventoryItem(Product p, Long warehouseId, int qty, BigDecimal cost,
                                             OperationType opType, AssemblyOrder order, Date now) {
        InventoryItem item = new InventoryItem();
        item.setOrderId(order.getId());
        item.setProductId(p.getId());
        item.setWarehouseId(warehouseId);
        item.setOperationType(opType);
        item.setQuantity(qty);
        item.setBaseUnitId(p.getUnitId());
        item.setMerchantId(order.getMerchantId());
        item.setAccountBookId(order.getAccountBookId());
        item.setBatchNumber(order.getOrderNo());
        item.setInventoryDate(now);
        item.setCreatedAt(LocalDateTime.now());
        item.setCreatedBy(order.getCreatedBy());
        item.setUnitPrice(qty > 0 ? cost.divide(BigDecimal.valueOf(qty), 6, RoundingMode.HALF_EVEN) : BigDecimal.ZERO);
        item.setSubtotal(cost);
        return item;
    }

    private void createReceiptBatch(Product p, AssemblyOrder order, int qty, BigDecimal unitCost,
                                    OperationType inType, Long itemId) {
        if (qty <= 0) {
            return;
        }
        CostingService.ReceiptRequest req = new CostingService.ReceiptRequest();
        req.setProductId(p.getId());
        req.setWarehouseId(order.getWarehouseId());
        req.setQty(qty);
        req.setUnitCost(unitCost);
        req.setInboundDate(java.time.LocalDate.now());
        req.setOrderId(order.getId());
        req.setOrderType(inType);
        req.setItemId(itemId);
        req.setSupplierId(null);
        req.setMerchantId(order.getMerchantId());
        req.setAccountBookId(order.getAccountBookId());
        costingService.createReceiptBatch(req);
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        AssemblyOrder original = assemblyOrderRepository.getById(id);
        Assert.isFalse(OrderStatus.已审核.equals(original.getOrderStatus()), "已审核单据不能删除~");
        jqf.delete(qAssemblyOrderItem)
                .where(qAssemblyOrderItem.assemblyOrderId.eq(id)
                        .and(qAssemblyOrderItem.merchantId.eq(merchantId))
                        .and(qAssemblyOrderItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qAssemblyOrder)
                .where(qAssemblyOrder.id.eq(id)
                        .and(qAssemblyOrder.merchantId.eq(merchantId))
                        .and(qAssemblyOrder.accountBookId.eq(accountBookId)))
                .execute();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qAssemblyOrder.orderStatus.eq(state));
            }
        }

        public void setType(AssemblyOrderType type) {
            if (type != null) {
                builder.and(qAssemblyOrder.orderType.eq(type));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qAssemblyOrder.orderNo.contains(filter));
            }
        }

        @Override
        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAssemblyOrder.merchantId.eq(merchantId));
            }
        }

        @Override
        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qAssemblyOrder.accountBookId.eq(accountBookId));
            }
        }
    }
}
