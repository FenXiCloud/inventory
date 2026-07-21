package com.flyemu.share.service.inventory;

import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryCostBatch;
import com.flyemu.share.entity.inventory.InventoryCostConsume;
import com.flyemu.share.enums.CostingMethod;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.repository.InventoryCostBatchRepository;
import com.flyemu.share.repository.InventoryCostConsumeRepository;
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
 * Uses Mockito.spy to stub {@code resolveMethod} so AbsService.bqf is never required.
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
    void toLocalDate_withSqlDate_doesNotThrow() {
        java.sql.Date sqlDate = java.sql.Date.valueOf(LocalDate.of(2024, 6, 15));
        LocalDate converted = CostingService.toLocalDate(sqlDate);
        assertNotNull(converted);
        assertEquals(LocalDate.of(2024, 6, 15), converted);

        assertNull(CostingService.toLocalDate(null));
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
