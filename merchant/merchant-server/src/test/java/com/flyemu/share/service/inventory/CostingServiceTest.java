package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryCostBatch;
import com.flyemu.share.entity.inventory.InventoryCostConsume;
import com.flyemu.share.enums.CostingMethod;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.repository.inventory.InventoryCostBatchRepository;
import com.flyemu.share.repository.inventory.InventoryCostConsumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Focused unit tests for {@link CostingService}.
 * <p>
 * Uses Mockito.spy to stub {@code resolveMethod} so BaseService.bqf is never required.
 */
@ExtendWith(MockitoExtension.class)
public class CostingServiceTest {

    @Mock
    private InventoryCostBatchRepository batchRepository;
    @Mock
    private InventoryCostConsumeRepository consumeRepository;
    @Mock
    private InventoryService inventoryService;

    private CostingService costingService;

    @BeforeEach
    void setUp() {
        costingService = spy(new CostingService(batchRepository, consumeRepository, inventoryService));
    }

    @Test
    void createReceiptBatch_withNonPositiveQty_returnsNull() {
        CostingService.ReceiptRequest req = new CostingService.ReceiptRequest();
        req.setQty(0);
        req.setProductId(1L);
        req.setWarehouseId(1L);

        assertNull(costingService.createReceiptBatch(req));

        req.setQty(-3);
        assertNull(costingService.createReceiptBatch(req));

        req.setQty(null);
        assertNull(costingService.createReceiptBatch(req));

        verify(batchRepository, never()).save(any());
    }

    @Test
    void issue_withNonPositiveQty_returnsZeroCost() {
        CostingService.IssueRequest req = new CostingService.IssueRequest();
        req.setQty(0);
        // null accountBookId → resolveMethod returns 移动加权平均 without bqf
        req.setAccountBookId(null);

        CostingService.IssueResult result = costingService.issue(req);

        assertEquals(0, result.getQty());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getCostPrice()));
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getCostAmount()));
        assertEquals(CostingMethod.移动加权平均, result.getCostingMethod());
        assertTrue(result.getConsumes().isEmpty());
        verify(consumeRepository, never()).saveAll(any());
    }

    @Test
    void issue_fifo_consumesOldestBatchFirst() {
        doReturn(CostingMethod.先进先出).when(costingService).resolveMethod(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(20);
        inventory.setAverageCost(new BigDecimal("12.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        // Oldest layer has only 5 remain so issue(7) must spill into the next batch
        InventoryCostBatch older = batch(1L, LocalDate.of(2024, 1, 1), 5, new BigDecimal("10.00"));
        InventoryCostBatch newer = batch(2L, LocalDate.of(2024, 2, 1), 10, new BigDecimal("20.00"));
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of(older, newer));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CostingService.IssueRequest req = issueRequest(7, 20L, 10L, 100L, 200L);

        CostingService.IssueResult result = costingService.issue(req);

        // 5 × 10 + 2 × 20 = 90
        assertEquals(7, result.getQty());
        assertEquals(0, new BigDecimal("90.00").compareTo(result.getCostAmount()));
        assertEquals(CostingMethod.先进先出, result.getCostingMethod());
        assertEquals(0, older.getQtyRemain());
        assertEquals(8, newer.getQtyRemain());
        assertTrue(older.getClosed());
        assertEquals(2, result.getConsumes().size());
        assertEquals(1L, result.getConsumes().get(0).getBatchId());
        assertEquals(5, result.getConsumes().get(0).getQty());
        assertEquals(2L, result.getConsumes().get(1).getBatchId());
        assertEquals(2, result.getConsumes().get(1).getQty());
    }

    @Test
    void issue_movingAverage_usesInventoryAverageCost() {
        doReturn(CostingMethod.移动加权平均).when(costingService).resolveMethod(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(50);
        inventory.setAverageCost(new BigDecimal("15.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        InventoryCostBatch layer = batch(1L, LocalDate.of(2024, 1, 1), 50, new BigDecimal("10.00"));
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of(layer));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CostingService.IssueResult result = costingService.issue(issueRequest(4, 20L, 10L, 100L, 200L));

        assertEquals(4, result.getQty());
        assertEquals(0, new BigDecimal("60.00").compareTo(result.getCostAmount()));
        assertEquals(0, new BigDecimal("15.000000").compareTo(result.getCostPrice()));
        assertEquals(CostingMethod.移动加权平均, result.getCostingMethod());
        assertEquals(46, layer.getQtyRemain());
    }

    @Test
    void reverseIssue_restoresQtyRemain() {
        InventoryCostBatch batch = batch(9L, LocalDate.of(2024, 1, 1), 3, new BigDecimal("10.00"));
        batch.setQtyIn(10);
        batch.setTotalCostRemain(new BigDecimal("30.00"));
        batch.setClosed(false);

        InventoryCostConsume consume = new InventoryCostConsume();
        consume.setBatchId(9L);
        consume.setQty(4);
        consume.setCostAmount(new BigDecimal("40.00"));
        consume.setCostingMethod(CostingMethod.先进先出);
        consume.setProductId(20L);
        consume.setWarehouseId(10L);

        when(consumeRepository.findByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                55L, OperationType.销售出库, 100L, 200L))
                .thenReturn(List.of(consume));
        when(batchRepository.findById(9L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        costingService.reverseIssue(55L, OperationType.销售出库, 100L, 200L);

        assertEquals(7, batch.getQtyRemain());
        assertEquals(0, new BigDecimal("70.00").compareTo(batch.getTotalCostRemain()));
        assertFalse(batch.getClosed());

        ArgumentCaptor<InventoryCostBatch> captor = ArgumentCaptor.forClass(InventoryCostBatch.class);
        verify(batchRepository).save(captor.capture());
        assertEquals(7, captor.getValue().getQtyRemain());
        verify(consumeRepository).deleteByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                55L, OperationType.销售出库, 100L, 200L);
    }

    @Test
    void issue_fifo_costPriceIsBlendedAverage() {
        doReturn(CostingMethod.先进先出).when(costingService).resolveMethod(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(20);
        inventory.setAverageCost(new BigDecimal("12.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        InventoryCostBatch older = batch(1L, LocalDate.of(2024, 1, 1), 5, new BigDecimal("10.00"));
        InventoryCostBatch newer = batch(2L, LocalDate.of(2024, 2, 1), 10, new BigDecimal("20.00"));
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of(older, newer));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // 5×10 + 2×20 = 90 → unit = 90/7 ≈ 12.857143
        CostingService.IssueResult result = costingService.issue(issueRequest(7, 20L, 10L, 100L, 200L));

        assertEquals(0, new BigDecimal("12.857143").compareTo(result.getCostPrice()));
        // older fully consumed → remain cost cleared to 0
        assertEquals(0, older.getQtyRemain());
        assertEquals(0, BigDecimal.ZERO.compareTo(older.getTotalCostRemain()));
        // newer: remain 8, cost remain 160
        assertEquals(0, new BigDecimal("160.00").compareTo(newer.getTotalCostRemain()));
    }

    @Test
    void issue_movingAverage_ignoresLayerUnitCost() {
        doReturn(CostingMethod.移动加权平均).when(costingService).resolveMethod(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(15);
        // 加权后均价 12：例如先入10@10 + 再入5@16 → (100+80)/15 = 12
        inventory.setAverageCost(new BigDecimal("12.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        InventoryCostBatch older = batch(1L, LocalDate.of(2024, 1, 1), 10, new BigDecimal("10.00"));
        InventoryCostBatch newer = batch(2L, LocalDate.of(2024, 2, 1), 5, new BigDecimal("16.00"));
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of(older, newer));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // 出库 6：移动加权成本 = 12×6 = 72（不是 FIFO 的 10×6=60）
        CostingService.IssueResult result = costingService.issue(issueRequest(6, 20L, 10L, 100L, 200L));

        assertEquals(6, result.getQty());
        assertEquals(0, new BigDecimal("72.00").compareTo(result.getCostAmount()));
        assertEquals(0, new BigDecimal("12.000000").compareTo(result.getCostPrice()));
        // 仍按先进先出顺序扣批次数量
        assertEquals(4, older.getQtyRemain());
        assertEquals(5, newer.getQtyRemain());
    }

    @Test
    void issue_fifo_singleBatchExactConsume() {
        doReturn(CostingMethod.先进先出).when(costingService).resolveMethod(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(8);
        inventory.setAverageCost(new BigDecimal("10.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        InventoryCostBatch layer = batch(1L, LocalDate.of(2024, 1, 1), 8, new BigDecimal("10.00"));
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of(layer));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        CostingService.IssueResult result = costingService.issue(issueRequest(8, 20L, 10L, 100L, 200L));

        assertEquals(8, result.getQty());
        assertEquals(0, new BigDecimal("80.00").compareTo(result.getCostAmount()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(result.getCostPrice()));
        assertEquals(0, layer.getQtyRemain());
        assertTrue(layer.getClosed());
        assertEquals(0, BigDecimal.ZERO.compareTo(layer.getTotalCostRemain()));
    }

    @Test
    void reverseIssue_movingAverage_restoresByLayerUnitCost() {
        InventoryCostBatch batch = batch(9L, LocalDate.of(2024, 1, 1), 3, new BigDecimal("10.00"));
        batch.setQtyIn(10);
        batch.setTotalCostRemain(new BigDecimal("30.00"));

        // 移动加权出库时记账成本可能是均价 12，回补应按层入库单价 10
        InventoryCostConsume consume = new InventoryCostConsume();
        consume.setBatchId(9L);
        consume.setQty(4);
        consume.setCostAmount(new BigDecimal("48.00"));
        consume.setCostingMethod(CostingMethod.移动加权平均);

        when(consumeRepository.findByOutboundOrderIdAndOutboundOrderTypeAndMerchantIdAndAccountBookId(
                55L, OperationType.销售出库, 100L, 200L))
                .thenReturn(List.of(consume));
        when(batchRepository.findById(9L)).thenReturn(Optional.of(batch));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));

        costingService.reverseIssue(55L, OperationType.销售出库, 100L, 200L);

        assertEquals(7, batch.getQtyRemain());
        assertEquals(0, new BigDecimal("70.00").compareTo(batch.getTotalCostRemain()));
    }

    @Test
    void createReceiptBatch_setsQtyAndCost() {
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> {
            InventoryCostBatch b = inv.getArgument(0);
            b.setId(100L);
            return b;
        });

        CostingService.ReceiptRequest req = new CostingService.ReceiptRequest();
        req.setQty(10);
        req.setUnitCost(new BigDecimal("8.50"));
        req.setProductId(1L);
        req.setWarehouseId(2L);
        req.setOrderId(3L);
        req.setOrderType(OperationType.采购入库);
        req.setInboundDate(LocalDate.of(2024, 3, 1));
        req.setMerchantId(100L);
        req.setAccountBookId(200L);

        InventoryCostBatch saved = costingService.createReceiptBatch(req);

        assertNotNull(saved);
        assertEquals(10, saved.getQtyIn());
        assertEquals(10, saved.getQtyRemain());
        assertEquals(0, new BigDecimal("8.50").compareTo(saved.getUnitCost()));
        assertEquals(0, new BigDecimal("85.00").compareTo(saved.getTotalCostRemain()));
        assertFalse(saved.getClosed());
    }

    @Test
    void issue_allowNegative_fifo_partialOverageUsesAverageCost() {
        doReturn(CostingMethod.先进先出).when(costingService).resolveMethod(any());
        doReturn(true).when(costingService).allowNegativeStock(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(3);
        inventory.setAverageCost(new BigDecimal("10.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        InventoryCostBatch layer = batch(1L, LocalDate.of(2024, 1, 1), 3, new BigDecimal("10.00"));
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of(layer));
        when(batchRepository.save(any(InventoryCostBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // 库存 3，出库 5：3×10 + 2×10(均价) = 50
        CostingService.IssueResult result = costingService.issue(issueRequest(5, 20L, 10L, 100L, 200L));

        assertEquals(5, result.getQty());
        assertEquals(0, new BigDecimal("50.00").compareTo(result.getCostAmount()));
        assertEquals(0, new BigDecimal("10.000000").compareTo(result.getCostPrice()));
        assertEquals(0, layer.getQtyRemain());
        assertEquals(2, result.getConsumes().size());
        assertEquals(0L, result.getConsumes().get(1).getBatchId());
        assertEquals(2, result.getConsumes().get(1).getQty());
    }

    @Test
    void issue_allowNegative_movingAverage_zeroStockUsesLastBatchCost() {
        doReturn(CostingMethod.移动加权平均).when(costingService).resolveMethod(any());
        doReturn(true).when(costingService).allowNegativeStock(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(0);
        inventory.setAverageCost(BigDecimal.ZERO);
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdAndQtyRemainGreaterThanOrderByInboundDateAscIdAsc(
                eq(20L), eq(10L), eq(100L), eq(200L), anyInt()))
                .thenReturn(List.of());
        InventoryCostBatch last = batch(9L, LocalDate.of(2024, 1, 1), 0, new BigDecimal("18.00"));
        last.setQtyRemain(0);
        last.setClosed(true);
        when(batchRepository.findByProductIdAndWarehouseIdAndMerchantIdAndAccountBookIdOrderByInboundDateDescIdDesc(
                eq(20L), eq(10L), eq(100L), eq(200L)))
                .thenReturn(List.of(last));
        when(consumeRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        // 零库存出库 4：回退最近批次单价 18 → 成本 72
        CostingService.IssueResult result = costingService.issue(issueRequest(4, 20L, 10L, 100L, 200L));

        assertEquals(4, result.getQty());
        assertEquals(0, new BigDecimal("72.00").compareTo(result.getCostAmount()));
        assertEquals(0, new BigDecimal("18.000000").compareTo(result.getCostPrice()));
    }

    @Test
    void issue_disallowNegative_throwsWhenInsufficient() {
        doReturn(CostingMethod.先进先出).when(costingService).resolveMethod(any());
        doReturn(false).when(costingService).allowNegativeStock(any());

        Inventory inventory = new Inventory();
        inventory.setCurrentQuantity(2);
        inventory.setAverageCost(new BigDecimal("10.00"));
        when(inventoryService.findByWarehouseIdAndProductId(10L, 20L)).thenReturn(inventory);

        org.junit.jupiter.api.Assertions.assertThrows(com.flyemu.share.exception.ServiceException.class,
                () -> costingService.issue(issueRequest(5, 20L, 10L, 100L, 200L)));
    }



    private static InventoryCostBatch batch(Long id, LocalDate inboundDate, int qtyRemain, BigDecimal unitCost) {
        InventoryCostBatch b = new InventoryCostBatch();
        b.setId(id);
        b.setInboundDate(inboundDate);
        b.setQtyIn(qtyRemain);
        b.setQtyRemain(qtyRemain);
        b.setUnitCost(unitCost);
        b.setTotalCostRemain(unitCost.multiply(BigDecimal.valueOf(qtyRemain)));
        b.setClosed(false);
        return b;
    }

    private static CostingService.IssueRequest issueRequest(int qty, Long productId, Long warehouseId,
                                                            Long merchantId, Long accountBookId) {
        CostingService.IssueRequest req = new CostingService.IssueRequest();
        req.setQty(qty);
        req.setProductId(productId);
        req.setWarehouseId(warehouseId);
        req.setMerchantId(merchantId);
        req.setAccountBookId(accountBookId);
        req.setOrderId(88L);
        req.setOrderType(OperationType.销售出库);
        req.setItemId(1L);
        return req;
    }
}
