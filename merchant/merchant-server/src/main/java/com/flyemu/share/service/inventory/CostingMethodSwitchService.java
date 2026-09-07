package com.flyemu.share.service.inventory;

import cn.hutool.core.lang.Dict;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryCostBatch;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.entity.inventory.QInventoryCostBatch;
import com.flyemu.share.entity.inventory.QInventoryItem;
import com.flyemu.share.entity.setting.QAccountBook;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.enums.CostingMethod;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.inventory.InventoryCostBatchRepository;
import com.flyemu.share.repository.inventory.InventoryRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.setting.SystemLogService;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 成本核算方法切换：在账套参数保存（costAccounting 实际变化）的同事务内做批次期初衔接。
 * <p>
 * 移动平均 ➡ 先进先出：旧数量层（移动平均期间建的零成本层）剩余数量清零封闭——出库取层
 * 查询只看 qtyRemain&gt;0 不看 closed，仅置 closed 会导致 FIFO 优先消耗零成本层；
 * 结余库存按账面加权均价建「切换批次」作为第一批次，后续按批次单位成本核算。
 * <p>
 * 先进先出 ➡ 移动平均：批次剩余加权成本回写账面余额作为期初均价（时点结余项为期初），
 * 批次清零封闭，后续入库/出库自动按移动平均走（createReceiptBatch/issue 每单实时读参数）。
 * <p>
 * 不触碰历史单据与耗用行（consume 行存有 costingMethod 快照，历史自洽），
 * 也不调用 rebuildCostChain（全量回放会重写已结账期间数据，仅保留为运维工具）。
 * <p>
 * 已知限制：切换后若反审核「切换前」的出库单，reverseIssue 会按耗用回补复活旧的零成本层，
 * 该守卫列入后续迭代；确认弹窗文案中已作警示。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CostingMethodSwitchService extends BaseService {

    private static final QInventory qInventory = QInventory.inventory;
    private static final QInventoryItem qInventoryItem = QInventoryItem.inventoryItem;
    private static final QInventoryCostBatch qBatch = QInventoryCostBatch.inventoryCostBatch;
    private static final QAccountBook qAccountBook = QAccountBook.accountBook;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /** 会写入库存流水（InventoryItem）的出入库操作类型集合 */
    private static final Set<OperationType> STOCK_FLOW_TYPES = Stream.concat(
            CheckoutService.INBOUND_TYPES.stream(), CheckoutService.OUTBOUND_TYPES.stream())
            .collect(Collectors.toUnmodifiableSet());

    private final InventoryCostBatchRepository batchRepository;
    private final InventoryRepository inventoryRepository;
    private final SystemLogService systemLogService;

    /**
     * 当前期间起始日：结账日+1；从未结账则账套启用日期。
     */
    public LocalDate periodFromDate(Long merchantId, Long accountBookId) {
        Tuple t = bqf.select(qAccountBook.checkoutDate, qAccountBook.startDate).from(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .fetchFirst();
        if (t == null) {
            return null;
        }
        LocalDate checkout = t.get(qAccountBook.checkoutDate);
        return checkout != null ? checkout.plusDays(1) : t.get(qAccountBook.startDate);
    }

    /**
     * 本期间内已审核的出入库流水条数（流水在审核时写入、反审核删除，count 即已审核量）。
     */
    public long countPeriodStockFlows(Long merchantId, Long accountBookId) {
        LocalDate from = periodFromDate(merchantId, accountBookId);
        if (from == null) {
            return 0;
        }
        Long count = bqf.selectFrom(qInventoryItem)
                .where(qInventoryItem.merchantId.eq(merchantId)
                        .and(qInventoryItem.accountBookId.eq(accountBookId))
                        .and(qInventoryItem.inventoryDate.goe(java.sql.Date.valueOf(from)))
                        .and(qInventoryItem.operationType.in(STOCK_FLOW_TYPES)))
                .fetchCount();
        return count == null ? 0 : count;
    }

    /**
     * 切换前置确认闸：期间内已有已审核流水且未确认 → 抛错（417 透传消息），前端弹警告后带确认重发。
     */
    public void assertConfirmed(Long merchantId, Long accountBookId, boolean confirmed) {
        long count = countPeriodStockFlows(merchantId, accountBookId);
        if (count > 0 && !confirmed) {
            throw new ServiceException("当前期间（自 " + periodFromDate(merchantId, accountBookId)
                    + " 起）已有 " + count + " 条已审核出入库流水，期间内切换成本核算方法存在新旧口径衔接风险，"
                    + "建议结账后在期间初切换；如仍要切换，请在弹窗中确认~");
        }
    }

    /**
     * 前端二次确认前的只读预览：期间起始日 + 已审核流水数（用于警告文案分级）。
     */
    public Dict preview(Long merchantId, Long accountBookId) {
        return Dict.create()
                .set("periodStart", periodFromDate(merchantId, accountBookId))
                .set("periodFlowCount", countPeriodStockFlows(merchantId, accountBookId));
    }

    /**
     * 执行切换衔接（与参数保存同事务）。返回摘要供前端 toast。
     */
    @Transactional
    public Dict applySwitch(Long merchantId, Long accountBookId, Integer fromMethod, Integer toMethod, Long adminId) {
        CostingMethod target = CostingMethod.fromParam(toMethod);
        boolean toFifo = target == CostingMethod.先进先出;

        List<Inventory> inventories = jqf.selectFrom(qInventory)
                .where(qInventory.merchantId.eq(merchantId).and(qInventory.accountBookId.eq(accountBookId)))
                .fetch();
        List<InventoryCostBatch> openBatches = bqf.selectFrom(qBatch)
                .where(qBatch.merchantId.eq(merchantId)
                        .and(qBatch.accountBookId.eq(accountBookId))
                        .and(qBatch.qtyRemain.gt(0)))
                .fetch();
        Map<String, List<InventoryCostBatch>> layersByKey = openBatches.stream()
                .collect(Collectors.groupingBy(b -> comboKey(b.getProductId(), b.getWarehouseId())));
        Map<String, List<Inventory>> invsByKey = inventories.stream()
                .collect(Collectors.groupingBy(i -> comboKey(i.getProductId(), i.getWarehouseId())));

        List<InventoryCostBatch> closedLayers = new ArrayList<>();
        List<InventoryCostBatch> newLayers = new ArrayList<>();
        List<Inventory> updatedInventories = new ArrayList<>();
        int affectedCombos = 0;
        int zeroCostCombos = 0;
        LocalDate today = LocalDate.now();

        for (Map.Entry<String, List<Inventory>> entry : invsByKey.entrySet()) {
            List<Inventory> rows = entry.getValue();
            List<InventoryCostBatch> layers = layersByKey.remove(entry.getKey());
            if (layers == null) {
                layers = List.of();
            }
            int totalQty = rows.stream().map(i -> i.getCurrentQuantity() == null ? 0 : i.getCurrentQuantity())
                    .mapToInt(Integer::intValue).sum();

            if (toFifo) {
                // MA➡FIFO：账面加权均价作为切换批次单位成本；均价为 0 回退最近非零批次成本
                BigDecimal bookCost = rows.stream().map(i -> i.getTotalCost() == null ? BigDecimal.ZERO : i.getTotalCost())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                // 先取回退成本（清零后 layers 无成本可查）
                BigDecimal unitCost = null;
                if (totalQty >= 1) {
                    unitCost = bookCost.divide(BigDecimal.valueOf(totalQty), 6, RoundingMode.HALF_EVEN);
                    if (unitCost.compareTo(BigDecimal.ZERO) == 0) {
                        unitCost = recentNonZeroUnitCost(rows.get(0).getProductId(), rows.get(0).getWarehouseId(),
                                merchantId, accountBookId);
                    }
                }
                for (InventoryCostBatch layer : layers) {
                    closeLayer(layer);
                    closedLayers.add(layer);
                }
                if (totalQty >= 1) {
                    if (unitCost.compareTo(BigDecimal.ZERO) == 0) {
                        zeroCostCombos++;
                        log.warn("成本法切换：品仓 combo={} 结余 {} 但均价为0，切换批次按零成本登记（建议后续成本调整）", entry.getKey(), totalQty);
                    }
                    newLayers.add(buildSwitchLayer(rows.get(0).getProductId(), rows.get(0).getWarehouseId(),
                            totalQty, unitCost, today, merchantId, accountBookId));
                    affectedCombos++;
                } else if (!layers.isEmpty()) {
                    affectedCombos++;
                }
            } else {
                // FIFO➡MA：剩余批次加权成本按期初均价回写账面，再封闭批次
                if (layers.isEmpty()) {
                    continue;
                }
                BigDecimal remainCost = layers.stream()
                        .map(l -> l.getTotalCostRemain() == null ? BigDecimal.ZERO : l.getTotalCostRemain())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                for (InventoryCostBatch layer : layers) {
                    closeLayer(layer);
                    closedLayers.add(layer);
                }
                if (totalQty > 0) {
                    // 按行数量比例分摊剩余成本（货位多行时口径=数量占比；实践中品仓单行）
                    BigDecimal allocated = BigDecimal.ZERO;
                    for (int i = 0; i < rows.size(); i++) {
                        Inventory row = rows.get(i);
                        int rowQty = row.getCurrentQuantity() == null ? 0 : row.getCurrentQuantity();
                        BigDecimal rowCost;
                        if (i == rows.size() - 1) {
                            rowCost = remainCost.subtract(allocated);
                        } else {
                            rowCost = remainCost.multiply(BigDecimal.valueOf(rowQty))
                                    .divide(BigDecimal.valueOf(totalQty), 2, RoundingMode.HALF_EVEN);
                            allocated = allocated.add(rowCost);
                        }
                        row.setTotalCost(rowCost);
                        // 均价为单价族：6 位小数（与批次 unit_cost 同口径），避免低值品（成本÷大数量<0.005）被 2 位舍成 0
                        row.setAverageCost(rowCost.divide(BigDecimal.valueOf(rowQty), 6, RoundingMode.HALF_EVEN));
                        row.setUpdatedAt(LocalDateTime.now());
                        updatedInventories.add(row);
                    }
                }
                affectedCombos++;
            }
        }
        // 有批次但无账面余额行的孤儿层（异常数据）：一并清零封闭，防止 FIFO 误消耗
        for (List<InventoryCostBatch> orphans : layersByKey.values()) {
            for (InventoryCostBatch layer : orphans) {
                closeLayer(layer);
                closedLayers.add(layer);
            }
            affectedCombos++;
        }

        if (!closedLayers.isEmpty()) {
            batchRepository.saveAll(closedLayers);
        }
        if (!newLayers.isEmpty()) {
            batchRepository.saveAll(newLayers);
        }
        if (!updatedInventories.isEmpty()) {
            inventoryRepository.saveAll(updatedInventories);
        }

        String fromLabel = CostingMethod.fromParam(fromMethod).name();
        String toLabel = CostingMethod.fromParam(toMethod).name();
        String description = "成本核算方法由「" + fromLabel + "」切换为「" + toLabel + "」：完成 "
                + affectedCombos + " 个品仓组合的批次期初衔接（封闭批次层 " + closedLayers.size()
                + " 个、新建衔接层 " + newLayers.size() + " 个"
                + (zeroCostCombos > 0 ? "，其中 " + zeroCostCombos + " 个组合均价为0按零成本登记" : "") + "）";
        try {
            systemLogService.record("账套参数", SystemLog.OperationType.修改, description,
                    null, "merchantId=" + merchantId + ",accountBookId=" + accountBookId
                            + ",from=" + fromMethod + ",to=" + toMethod,
                    accountBookId, adminId, merchantId, accountBookId);
        } catch (Exception e) {
            log.warn("记录成本法切换日志失败：{}", e.getMessage());
        }
        log.info("成本法切换完成：商户={}，账套={}，{}", merchantId, accountBookId, description);

        return Dict.create()
                .set("switched", true)
                .set("fromMethod", fromLabel)
                .set("toMethod", toLabel)
                .set("affectedCombos", affectedCombos)
                .set("closedLayers", closedLayers.size())
                .set("createdLayers", newLayers.size());
    }

    private String comboKey(Long productId, Long warehouseId) {
        return productId + "-" + warehouseId;
    }

    /** 封闭批次层：数量与剩余金额清零（取层查询不看 closed，必须清零才不会被 FIFO 消耗） */
    private void closeLayer(InventoryCostBatch layer) {
        layer.setQtyRemain(0);
        layer.setTotalCostRemain(BigDecimal.ZERO);
        layer.setClosed(true);
    }

    /** 账面均价为 0 时的回退：最近一次入库中非零的单位成本 */
    private BigDecimal recentNonZeroUnitCost(Long productId, Long warehouseId, Long merchantId, Long accountBookId) {
        List<InventoryCostBatch> history = batchRepository
                .findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdOrderByInboundDateDescIdDesc(
                        productId, warehouseId, merchantId, accountBookId);
        if (history != null) {
            for (InventoryCostBatch batch : history) {
                if (batch.getUnitCost() != null && batch.getUnitCost().compareTo(BigDecimal.ZERO) != 0) {
                    return batch.getUnitCost().setScale(6, RoundingMode.HALF_EVEN);
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private InventoryCostBatch buildSwitchLayer(Long productId, Long warehouseId, int qty,
                                                BigDecimal unitCost, LocalDate inboundDate,
                                                Long merchantId, Long accountBookId) {
        InventoryCostBatch batch = new InventoryCostBatch();
        String batchNo = "SWITCH-" + accountBookId + "-" + LocalDateTime.now().format(TS) + "-" + productId;
        batch.setBatchNo(batchNo.length() > 64 ? batchNo.substring(0, 64) : batchNo);
        batch.setProductId(productId);
        batch.setWarehouseId(warehouseId);
        batch.setInboundDate(inboundDate);
        batch.setInboundOrderId(0L);
        batch.setInboundOrderType(OperationType.期初库存);
        batch.setInboundItemId(null);
        batch.setQtyIn(qty);
        batch.setQtyRemain(qty);
        batch.setUnitCost(unitCost);
        batch.setTotalCostRemain(unitCost.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_EVEN));
        batch.setClosed(false);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setMerchantId(merchantId);
        batch.setAccountBookId(accountBookId);
        return batch;
    }
}
