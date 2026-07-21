package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryCostBatch;
import com.flyemu.share.entity.inventory.InventoryCostConsume;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.enums.CostingMethod;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.InventoryCostBatchRepository;
import com.flyemu.share.repository.InventoryCostConsumeRepository;
import com.flyemu.share.service.AbsService;
import lombok.Data;
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
import java.util.Objects;

/**
 * 统一成本核算：入库建批次；出库扣批次数量，按移动加权/FIFO 估值。
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CostingService extends AbsService {

    private static final QAccountBookParameters Q_PARAMS = QAccountBookParameters.accountBookParameters;
    private static final DateTimeFormatter BATCH_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final InventoryCostBatchRepository batchRepository;
    private final InventoryCostConsumeRepository consumeRepository;
    private final InventoryService inventoryService;

    public CostingMethod resolveMethod(Long accountBookId) {
        if (accountBookId == null) {
            return CostingMethod.移动加权平均;
        }
        AccountBookParameters params = bqf.selectFrom(Q_PARAMS)
                .where(Q_PARAMS.accountBookId.eq(Math.toIntExact(accountBookId)))
                .fetchFirst();
        return CostingMethod.fromParam(params == null ? null : params.getCostAccounting());
    }

    public boolean allowNegativeStock(Long accountBookId) {
        if (accountBookId == null) {
            return false;
        }
        AccountBookParameters params = bqf.selectFrom(Q_PARAMS)
                .where(Q_PARAMS.accountBookId.eq(Math.toIntExact(accountBookId)))
                .fetchFirst();
        // availableInventory: 1=是允许负库存, 2=否
        return params != null && params.getAvailableInventory() != null && params.getAvailableInventory() == 1;
    }

    /**
     * 入库建批次（采购/其他入库等审核时调用）
     */
    @Transactional
    public InventoryCostBatch createReceiptBatch(ReceiptRequest req) {
        if (req.getQty() == null || req.getQty() <= 0) {
            return null;
        }
        BigDecimal unitCost = defaultCost(req.getUnitCost());
        BigDecimal total = unitCost.multiply(BigDecimal.valueOf(req.getQty())).setScale(2, RoundingMode.HALF_EVEN);

        InventoryCostBatch batch = new InventoryCostBatch();
        batch.setBatchNo(buildBatchNo(req.getOrderType(), req.getOrderId()));
        batch.setProductId(req.getProductId());
        batch.setWarehouseId(req.getWarehouseId());
        batch.setInboundDate(req.getInboundDate() != null ? req.getInboundDate() : LocalDate.now());
        batch.setInboundOrderId(req.getOrderId());
        batch.setInboundOrderType(req.getOrderType());
        batch.setInboundItemId(req.getItemId());
        batch.setQtyIn(req.getQty());
        batch.setQtyRemain(req.getQty());
        batch.setUnitCost(unitCost);
        batch.setTotalCostRemain(total);
        batch.setSupplierId(req.getSupplierId());
        batch.setClosed(false);
        batch.setCreatedAt(LocalDateTime.now());
        batch.setMerchantId(req.getMerchantId());
        batch.setAccountBookId(req.getAccountBookId());
        return batchRepository.save(batch);
    }

    /**
     * 反审入库：批次未被消耗才可删除
     */
    @Transactional
    public void reverseReceipt(Long orderId, OperationType orderType, Long merchantId, Long accountBookId) {
        List<InventoryCostBatch> batches = batchRepository
                .findByInboundOrderIdAndInboundOrderTypeAndMerchantIdAndAccountBookId(
                        orderId, orderType, merchantId, accountBookId);
        for (InventoryCostBatch batch : batches) {
            if (batch.getQtyRemain() == null || !batch.getQtyRemain().equals(batch.getQtyIn())) {
                throw new ServiceException("批次「" + batch.getBatchNo() + "」已有出库消耗，无法反审核入库单");
            }
        }
        batchRepository.deleteAll(batches);
    }

    /**
     * 出库：扣批次数量并按成本法计算成本
     */
    @Transactional
    public IssueResult issue(IssueRequest req) {
        IssueResult empty = new IssueResult();
        empty.setQty(0);
        empty.setCostPrice(BigDecimal.ZERO);
        empty.setCostAmount(BigDecimal.ZERO);
        empty.setCostingMethod(resolveMethod(req.getAccountBookId()));
        empty.setConsumes(List.of());
        if (req.getQty() == null || req.getQty() <= 0) {
            return empty;
        }
        CostingMethod method = empty.getCostingMethod();
        Inventory inventory = inventoryService.findByWarehouseIdAndProductId(req.getWarehouseId(), req.getProductId());
        BigDecimal averageCost = inventory != null && inventory.getAverageCost() != null
                ? inventory.getAverageCost() : BigDecimal.ZERO;
        int onHand = inventory != null && inventory.getCurrentQuantity() != null ? inventory.getCurrentQuantity() : 0;

        if (req.getQty() > onHand && !allowNegativeStock(req.getAccountBookId())) {
            throw new ServiceException("库存不足，无法出库");
        }

        List<InventoryCostBatch> layers = batchRepository
                .findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                        req.getProductId(), req.getWarehouseId(), req.getMerchantId(), req.getAccountBookId(), 0);

        int need = req.getQty();
        List<LayerTake> takes = new ArrayList<>();
        for (InventoryCostBatch layer : layers) {
            if (need <= 0) {
                break;
            }
            int remain = layer.getQtyRemain() == null ? 0 : layer.getQtyRemain();
            if (remain <= 0) {
                continue;
            }
            int take = Math.min(need, remain);
            takes.add(new LayerTake(layer, take));
            need -= take;
        }

        if (need > 0) {
            if (method == CostingMethod.移动加权平均 && onHand >= req.getQty()) {
                // 历史库存无完整批次时，移动加权仍可按平均成本出库
                takes.add(new LayerTake(null, need));
            } else if (allowNegativeStock(req.getAccountBookId())) {
                takes.add(new LayerTake(null, need));
            } else {
                throw new ServiceException("批次库存不足，无法出库（请先补录入库或切换成本法）");
            }
        }

        BigDecimal costAmount;
        if (method == CostingMethod.先进先出) {
            costAmount = BigDecimal.ZERO;
            for (LayerTake take : takes) {
                if (take.batch == null) {
                    costAmount = costAmount.add(averageCost.multiply(BigDecimal.valueOf(take.qty)));
                } else {
                    costAmount = costAmount.add(take.batch.getUnitCost().multiply(BigDecimal.valueOf(take.qty)));
                }
            }
            costAmount = costAmount.setScale(2, RoundingMode.HALF_EVEN);
        } else {
            costAmount = averageCost.multiply(BigDecimal.valueOf(req.getQty())).setScale(2, RoundingMode.HALF_EVEN);
        }

        BigDecimal costPrice = req.getQty() == 0 ? BigDecimal.ZERO
                : costAmount.divide(BigDecimal.valueOf(req.getQty()), 6, RoundingMode.HALF_EVEN);

        List<InventoryCostConsume> consumes = new ArrayList<>();
        int allocatedQty = 0;
        BigDecimal allocatedCost = BigDecimal.ZERO;
        for (int i = 0; i < takes.size(); i++) {
            LayerTake take = takes.get(i);
            boolean last = i == takes.size() - 1;
            BigDecimal lineCost;
            BigDecimal lineUnit;
            if (method == CostingMethod.先进先出) {
                lineUnit = take.batch != null ? take.batch.getUnitCost() : averageCost;
                lineCost = lineUnit.multiply(BigDecimal.valueOf(take.qty)).setScale(2, RoundingMode.HALF_EVEN);
            } else {
                // 移动加权：按数量分摊本笔 costAmount
                if (last) {
                    lineCost = costAmount.subtract(allocatedCost).setScale(2, RoundingMode.HALF_EVEN);
                } else {
                    lineCost = costAmount.multiply(BigDecimal.valueOf(take.qty))
                            .divide(BigDecimal.valueOf(req.getQty()), 2, RoundingMode.HALF_EVEN);
                    allocatedCost = allocatedCost.add(lineCost);
                }
                lineUnit = take.qty == 0 ? costPrice
                        : lineCost.divide(BigDecimal.valueOf(take.qty), 6, RoundingMode.HALF_EVEN);
            }

            if (take.batch != null) {
                int newRemain = take.batch.getQtyRemain() - take.qty;
                take.batch.setQtyRemain(newRemain);
                // 两种成本法都同步扣减层剩余金额，保证数量与金额一致
                BigDecimal remainCost = take.batch.getTotalCostRemain() == null ? BigDecimal.ZERO : take.batch.getTotalCostRemain();
                if (method == CostingMethod.先进先出) {
                    remainCost = remainCost.subtract(lineCost).setScale(2, RoundingMode.HALF_EVEN);
                } else if (take.batch.getQtyIn() != null && take.batch.getQtyIn() > 0) {
                    // 移动加权：按扣减数量比例扣剩余成本
                    BigDecimal unitLayer = take.batch.getUnitCost() == null ? BigDecimal.ZERO : take.batch.getUnitCost();
                    remainCost = remainCost.subtract(unitLayer.multiply(BigDecimal.valueOf(take.qty)))
                            .setScale(2, RoundingMode.HALF_EVEN);
                }
                if (remainCost.compareTo(BigDecimal.ZERO) < 0 || newRemain <= 0) {
                    remainCost = newRemain <= 0 ? BigDecimal.ZERO : remainCost.max(BigDecimal.ZERO);
                }
                take.batch.setTotalCostRemain(remainCost);
                take.batch.setClosed(newRemain <= 0);
                batchRepository.save(take.batch);
            }

            InventoryCostConsume consume = new InventoryCostConsume();
            consume.setOutboundOrderId(req.getOrderId());
            consume.setOutboundOrderType(req.getOrderType());
            consume.setOutboundItemId(req.getItemId());
            consume.setBatchId(take.batch != null ? take.batch.getId() : 0L);
            consume.setProductId(req.getProductId());
            consume.setWarehouseId(req.getWarehouseId());
            consume.setQty(take.qty);
            consume.setUnitCostUsed(lineUnit);
            consume.setCostAmount(lineCost);
            consume.setCostingMethod(method);
            consume.setCreatedAt(LocalDateTime.now());
            consume.setMerchantId(req.getMerchantId());
            consume.setAccountBookId(req.getAccountBookId());
            consumes.add(consume);
            allocatedQty += take.qty;
        }
        consumeRepository.saveAll(consumes);

        IssueResult result = new IssueResult();
        result.setCostPrice(costPrice);
        result.setCostAmount(costAmount);
        result.setCostingMethod(method);
        result.setConsumes(consumes);
        result.setQty(allocatedQty);
        return result;
    }

    /**
     * 汇总某出库单在指定品仓上的耗用成本（反审调拨等用）
     */
    public BigDecimal sumIssueCost(Long orderId, OperationType orderType, Long productId, Long warehouseId,
                                   Long merchantId, Long accountBookId) {
        return sumIssueCost(orderId, orderType, productId, warehouseId, null, merchantId, accountBookId);
    }

    public BigDecimal sumIssueCost(Long orderId, OperationType orderType, Long productId, Long warehouseId,
                                   Long outboundItemId, Long merchantId, Long accountBookId) {
        return consumeRepository
                .findByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                        orderId, orderType, merchantId, accountBookId)
                .stream()
                .filter(c -> Objects.equals(c.getProductId(), productId) && Objects.equals(c.getWarehouseId(), warehouseId))
                .filter(c -> outboundItemId == null || Objects.equals(c.getOutboundItemId(), outboundItemId))
                .map(c -> c.getCostAmount() == null ? BigDecimal.ZERO : c.getCostAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean hasIssueConsumes(Long orderId, OperationType orderType, Long outboundItemId,
                                    Long merchantId, Long accountBookId) {
        return consumeRepository
                .findByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                        orderId, orderType, merchantId, accountBookId)
                .stream()
                .anyMatch(c -> outboundItemId == null || Objects.equals(c.getOutboundItemId(), outboundItemId));
    }

    /** 安全将 Date 转为 LocalDate（兼容 java.sql.Date） */
    public static LocalDate toLocalDate(java.util.Date date) {
        if (date == null) {
            return null;
        }
        if (date instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate();
        }
        return date.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    /**
     * 反审出库：按耗用明细回补批次
     */
    @Transactional
    public void reverseIssue(Long orderId, OperationType orderType, Long merchantId, Long accountBookId) {
        List<InventoryCostConsume> consumes = consumeRepository
                .findByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                        orderId, orderType, merchantId, accountBookId);
        for (InventoryCostConsume consume : consumes) {
            if (consume.getBatchId() == null || consume.getBatchId() <= 0) {
                continue;
            }
            InventoryCostBatch batch = batchRepository.findById(consume.getBatchId()).orElse(null);
            if (batch == null) {
                continue;
            }
            int qty = consume.getQty() == null ? 0 : consume.getQty();
            batch.setQtyRemain((batch.getQtyRemain() == null ? 0 : batch.getQtyRemain()) + qty);
            // FIFO 回补记账成本；移动加权回补层入库成本×数量
            BigDecimal add;
            if (consume.getCostingMethod() == CostingMethod.先进先出) {
                add = consume.getCostAmount() == null ? BigDecimal.ZERO : consume.getCostAmount();
            } else {
                BigDecimal unit = batch.getUnitCost() == null ? BigDecimal.ZERO : batch.getUnitCost();
                add = unit.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_EVEN);
            }
            batch.setTotalCostRemain((batch.getTotalCostRemain() == null ? BigDecimal.ZERO : batch.getTotalCostRemain())
                    .add(add).setScale(2, RoundingMode.HALF_EVEN));
            batch.setClosed(false);
            batchRepository.save(batch);
        }
        consumeRepository.deleteByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                orderId, orderType, merchantId, accountBookId);
    }

    private String buildBatchNo(OperationType type, Long orderId) {
        return (type == null ? "IN" : type.name()) + "-" + orderId + "-" + LocalDateTime.now().format(BATCH_TS);
    }

    private BigDecimal defaultCost(BigDecimal cost) {
        return cost == null ? BigDecimal.ZERO : cost;
    }

    private record LayerTake(InventoryCostBatch batch, int qty) {
    }

    @Data
    public static class ReceiptRequest {
        private Long productId;
        private Long warehouseId;
        private Integer qty;
        private BigDecimal unitCost;
        private LocalDate inboundDate;
        private Long orderId;
        private OperationType orderType;
        private Long itemId;
        private Long supplierId;
        private Long merchantId;
        private Long accountBookId;
    }

    @Data
    public static class IssueRequest {
        private Long productId;
        private Long warehouseId;
        private Integer qty;
        private Long orderId;
        private OperationType orderType;
        private Long itemId;
        private Long merchantId;
        private Long accountBookId;
    }

    @Data
    public static class IssueResult {
        private Integer qty;
        private BigDecimal costPrice;
        private BigDecimal costAmount;
        private CostingMethod costingMethod;
        private List<InventoryCostConsume> consumes;
    }
}
