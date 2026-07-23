package com.flyemu.share.service.sales;

import com.flyemu.share.repository.sales.SalesOrderItemRepository;
import com.flyemu.share.repository.sales.SalesOutboundItemRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.repository.sales.SalesReturnItemRepository;
import com.flyemu.share.repository.sales.SalesReturnRepository;
import com.flyemu.share.common.TenantAware;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.sales.SalesReturnDto;
import com.flyemu.share.dto.sales.SalesReturnItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SalesReturnForm;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.inventory.CostingService;
import com.flyemu.share.service.inventory.InventoryService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReturnService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QSalesReturn qSalesReturn = QSalesReturn.salesReturn;
    private final static QSalesReturnItem qsalesReturnItem = QSalesReturnItem.salesReturnItem;

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;

    private final SalesReturnRepository salesReturnRepository;
    private final SalesReturnItemRepository salesReturnItemRepository;
    private final CodeSeedService codeSeedService;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesOutboundItemRepository salesOutboundItemRepository;

    private final SalesOrderItemRepository salesOrderItemRepository;

    private final PriceRecordService priceRecordService;

    private final InventoryService inventoryService;
    private final CostingService costingService;
    private final CustomerService customerService;

    public PageResults<SalesReturnDto> query(Page page, SalesReturnService.Query query) {
        long totalSize = bqf.selectFrom(qSalesReturn)
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesReturn)
                .select(qSalesReturn, qCustomer.name, qMerchantUser.name)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesReturn.createdBy))
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesReturn.customerId))
                .where(query.builder)
                .orderBy(qSalesReturn.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesReturnDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesReturnDto salesReturnDTO = BeanUtil.toBean(tuple.get(qSalesReturn), SalesReturnDto.class);
            salesReturnDTO.setCustomerName(tuple.get(qCustomer.name));
            salesReturnDTO.setCreatedName(tuple.get(qMerchantUser.name));
            //查询子表
            List<SalesReturnItem> salesReturnItemList = bqf.selectFrom(qsalesReturnItem)
                    .select(qsalesReturnItem)
                    .where(qsalesReturnItem.salesReturnId.eq(salesReturnDTO.getId()))
                    .fetch();
            List<SalesReturnItemDto> itemDTOs = new ArrayList<>();
            AtomicReference<BigDecimal> totalQuantity = new AtomicReference<>(BigDecimal.ZERO);
            salesReturnItemList.forEach(item -> {
                SalesReturnItemDto itemDTO = BeanUtil.toBean(item, SalesReturnItemDto.class);
                itemDTOs.add(itemDTO);
                BigDecimal quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v.add(quantity));
            });
            salesReturnDTO.setSalesReturnItemList(itemDTOs);
            salesReturnDTO.setTotalQuantity(totalQuantity);

            //查询关联的出库单
            List<String> salesOutboundList = bqf.selectFrom(qSalesOutbound)
                    .select(qSalesOutbound.orderNo)
                    .where(qSalesOutbound.returnOrderId.eq(salesReturnDTO.getId()))
                    .fetch();
            if (!CollectionUtils.isEmpty(salesOutboundList)) {
                salesReturnDTO.setSalesOutboundNos(String.join(",", salesOutboundList));
            }

            dtos.add(salesReturnDTO);
        });

        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public SalesReturn save(SalesReturnForm salesReturnForm, Long merchantId) {
        SalesReturn salesReturn = salesReturnForm.getSalesReturn();
        checkoutService.assertEditable(salesReturn.getMerchantId(), salesReturn.getAccountBookId(), salesReturn.getReturnDate());
        salesReturn.setMerchantId(merchantId);
        List<SalesReturnItem> salesReturnItemList = salesReturnForm.getSalesReturnItemList();
        Long id = salesReturn.getId();
        if (id != null) {
            //更新
            SalesReturn original = salesReturnRepository.getById(salesReturn.getId());

            //已审核单据不能修改
            OrderStatus orderStatus = original.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已审核)) {
                throw new InvalidContextException("已审核单据不能修改");
            }

            BeanUtil.copyProperties(salesReturn, original, CopyOptions.create().ignoreNullValue());
            SalesReturn update = salesReturnRepository.save(original);
            if (!CollectionUtils.isEmpty(salesReturnItemList)) {
                salesReturnItemList.forEach(item -> {
                    checkQuantity(item);
                    savePrice(item, update);
                    item.setSalesReturnId(update.getId());
                    item.setAccountBookId(salesReturn.getAccountBookId());
                    item.setMerchantId(salesReturn.getMerchantId());
                });
                //批量修改
                salesReturnItemRepository.saveAll(salesReturnItemList);
            }
            return update;
        } else {
            //状态初始化
            salesReturn.setOrderStatus(OrderStatus.已保存);
            //订单编号
            salesReturn.setOrderNo(codeSeedService.generateCode(salesReturn.getMerchantId(), salesReturn.getAccountBookId(), "销售退货单"));
            SalesReturn save = salesReturnRepository.save(salesReturn);
            if (!CollectionUtils.isEmpty(salesReturnItemList)) {
                salesReturnItemList.forEach(item -> {
                    checkQuantity(item);
                    //保存价格记录
                    savePrice(item, save);
                    item.setSalesReturnId(save.getId());
                    item.setAccountBookId(salesReturn.getAccountBookId());
                    item.setMerchantId(salesReturn.getMerchantId());
                    item.setCreatedBy(salesReturn.getCreatedBy());
                    item.setCreatedAt(salesReturn.getCreatedAt());
                });
                //批量保存
                salesReturnItemRepository.saveAll(salesReturnItemList);
            }
            //选择的源单不为空
            List<Long> selectSalesOutboundIdList = salesReturnForm.getSelectSalesOutboundIdList();
            if (!CollectionUtils.isEmpty(selectSalesOutboundIdList)) {

                List<SalesReturnItem> salesReturnItemListTemp = salesReturnForm.getSalesReturnItemList();
                for (SalesReturnItem item : salesReturnItemListTemp) {
                    //出库单id
                    Long outItemId = item.getOutItemId();
                    //出库单
                    SalesOutboundItem salesOutboundItem = salesOutboundItemRepository.getById(outItemId);
                    //订单id
                    Long tempId = salesOutboundItem.getTempId();
                    //订单
                    SalesOrderItem salesOrderItem = salesOrderItemRepository.getReferenceById(tempId);
                    //修改订单退货数量
                    salesOrderItem.setQuantityReturn(item.getQuantity());
                    salesOrderItemRepository.save(salesOrderItem);
                }
                List<Long> collect = selectSalesOutboundIdList.stream().distinct().toList();
                List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAllById(collect);
                salesOutboundList.forEach(order -> {
                    //退货单 关联 销售出库单
                    order.setReturnOrderId(save.getId());

                });
                salesOutboundRepository.saveAll(salesOutboundList);
            }
            return save;
        }

    }

    private void checkQuantity(SalesReturnItem item) {
        Long outItemId = item.getOutItemId();
        if (outItemId != null) {
            SalesOutboundItem salesOutboundItem = salesOutboundItemRepository.getById(outItemId);
            //出库单数量
            BigDecimal quantity = salesOutboundItem.getQuantity();
            //退货单数量
            BigDecimal quantity1 = item.getQuantity();
            if (quantity1.compareTo(quantity) > 0) {
                throw new InvalidContextException("退货数量不能大于出库数量");
            }
        }
    }

    private void savePrice(SalesReturnItem item, SalesReturn save) {
        //保存价格记录
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setOrderId(save.getId());
        priceRecord.setUnitPrice(item.getUnitPrice());
        priceRecord.setBaseUnitId(item.getBaseUnitId());
        priceRecord.setProductId(item.getProductId());
        priceRecord.setMerchantId(save.getMerchantId());
        priceRecord.setAccountBookId(save.getAccountBookId());
        priceRecord.setCustomerId(save.getCustomerId());
        priceRecord.setPriceSource(PriceSource.最近销售价格);
        priceRecord.setPriceType(PriceType.最近销售价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    @Transactional
    public void delete(Long salesReturnId, Long merchantId, Long accountBookId) {

        SalesReturn original = salesReturnRepository.getById(salesReturnId);
        //已审核单据不能删除
        OrderStatus orderStatus = original.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已审核)) {
            throw new InvalidContextException("已审核单据不能删除");
        }

        jqf.delete(qSalesReturn)
                .where(qSalesReturn.id.eq(salesReturnId).and(qSalesReturn.merchantId.eq(merchantId)).and(qSalesReturn.accountBookId.eq(accountBookId)))
                .execute();

        //删除退货单商品
        jqf.delete(qsalesReturnItem)
                .where(qsalesReturnItem.salesReturnId.eq(salesReturnId).and(qsalesReturnItem.merchantId.eq(merchantId)).and(qsalesReturnItem.accountBookId.eq(accountBookId)))
                .execute();

        //修改销售出库单 关联退货单
        jqf.update(qSalesOutbound)
                .setNull(qSalesOutbound.returnOrderId)
                .where(qSalesOutbound.returnOrderId.eq(original.getId()).and(qSalesOutbound.merchantId.eq(merchantId)).and(qSalesOutbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesReturn> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesReturn).where(qSalesReturn.merchantId.eq(merchantId).and(qSalesReturn.accountBookId.eq(accountBookId))).fetch();
    }

    public SalesReturnDto load(Long merchantId, Long orderId) {
        SalesReturn salesReturn = bqf.selectFrom(qSalesReturn)
                .where(qSalesReturn.merchantId.eq(merchantId).and(qSalesReturn.id.eq(orderId)))
                .fetchFirst();
        if (salesReturn == null) {
            throw new ServiceException("单据不存在");
        }
        SalesReturnDto dto = BeanUtil.toBean(salesReturn, SalesReturnDto.class);
        List<Tuple> fetch = jqf.selectFrom(qsalesReturnItem)
                .select(qsalesReturnItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qsalesReturnItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qsalesReturnItem.baseUnitId))
                .where(qsalesReturnItem.salesReturnId.eq(orderId)
                        .and(qsalesReturnItem.merchantId.eq(merchantId)))
                .orderBy(qsalesReturnItem.id.asc()).fetch();
        List<SalesReturnItemDto> salesReturnItemDTOList = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesReturnItemDto salesReturnItemDTO = BeanUtil.toBean(tuple.get(qsalesReturnItem), SalesReturnItemDto.class);
            salesReturnItemDTO.setProductName(tuple.get(qProduct.name));
            salesReturnItemDTO.setProductCode(tuple.get(qProduct.code));
            salesReturnItemDTO.setUnitName(tuple.get(qUnit.name));
            Long salesOutboundId = salesReturnItemDTO.getSalesOutboundId();
            if (salesOutboundId != null) {
                SalesOutbound salesOutbound = salesOutboundRepository.findById(salesOutboundId).orElse(null);
                if (salesOutbound != null) {
                    salesReturnItemDTO.setSalesOutboundNo(salesOutbound.getOrderNo());
                }
            }
            salesReturnItemDTOList.add(salesReturnItemDTO);
        });
        dto.setSalesReturnItemList(salesReturnItemDTOList);
        return dto;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择单据");
        }
        List<SalesReturn> salesReturnList = bqf.selectFrom(qSalesReturn)
                .where(qSalesReturn.merchantId.eq(merchantId).and(qSalesReturn.id.in(ids)))
                .fetch();
        if (salesReturnList.isEmpty()) {
            throw new ServiceException("未找到数据~");
        }
        salesReturnList.forEach(order -> {
            order.setOrderStatus(state);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(adminId);
        });
        salesReturnRepository.saveAll(salesReturnList);
        salesReturnList.forEach(order -> this.updateCustomerBalanceAndRecordFlow(order, state));
        salesReturnList.forEach(this::salesReturnToInventory);
    }

    private void updateCustomerBalanceAndRecordFlow(SalesReturn salesReturn, OrderStatus targetStatus) {
        Customer customer = customerService.selectByPrimaryKey(salesReturn.getCustomerId());
        if (customer == null) {
            throw new IllegalArgumentException("客户不存在");
        }
        BigDecimal amount = salesReturn.getFinalAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }

        CustomerFlow flow = getCustomerFlow(salesReturn, targetStatus);
        if (targetStatus == OrderStatus.已审核) {
            customer.setBalance(customer.getBalance().subtract(amount));
        } else {
            customer.setBalance(customer.getBalance().add(amount));
        }

        flow.setBalanceReceivables(customer.getBalance());
        customerService.updateTheBalance(customer, flow);
    }

    private CustomerFlow getCustomerFlow(SalesReturn salesReturn, OrderStatus targetStatus) {
        CustomerFlow flow = new CustomerFlow();
        flow.setCustomerId(salesReturn.getCustomerId());
        flow.setBusinessId(salesReturn.getId());
        flow.setBusinessNo(salesReturn.getOrderNo());
        flow.setBusinessDate(salesReturn.getReturnDate());

        BigDecimal finalAmount = salesReturn.getFinalAmount();

        flow.setRemarks(targetStatus == OrderStatus.已审核 ? "销售退货单审核通过" : "销售退货单反审核");

        if (targetStatus == OrderStatus.已审核) {
            flow.setCustomerFlowType(CustomerFlow.CustomerFlowType.销售退货单);
            flow.setSalesAmount(finalAmount.negate());
            flow.setReceivableAmount(finalAmount.negate());
            flow.setPreferentialAmount(salesReturn.getDiscountAmount() != null ? salesReturn.getDiscountAmount().negate() : BigDecimal.ZERO);
        } else {
            flow.setCustomerFlowType(CustomerFlow.CustomerFlowType.反审核_销售退货单);
            flow.setSalesAmount(finalAmount);
            flow.setReceivableAmount(finalAmount);
            flow.setPreferentialAmount(salesReturn.getDiscountAmount() != null ? salesReturn.getDiscountAmount() : BigDecimal.ZERO);
        }

        flow.setAccountBookId(salesReturn.getAccountBookId());
        flow.setMerchantId(salesReturn.getMerchantId());
        flow.setCreatedBy(salesReturn.getApprovedBy());
        flow.setCreatedAt(LocalDateTime.now());

        return flow;
    }

    private void salesReturnToInventory(SalesReturn original) {
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        List<SalesReturnItem> returnItems = jqf.selectFrom(qsalesReturnItem)
                .where(qsalesReturnItem.salesReturnId.eq(original.getId())).fetch();
        if (OrderStatus.已审核.equals(original.getOrderStatus())) {
            applyReturnReceiptCost(original, returnItems);
            this.getComputedInventory(returnItems, inventories, inventoryItems, original);
            inventories.forEach(item ->
                    inventoryService.computedInventory(item, true, original.getId(), OperationType.销售退货, inventoryItems));
            createReturnBatches(original, returnItems);
        } else {
            costingService.reverseReceipt(original.getId(), OperationType.销售退货,
                    original.getMerchantId(), original.getAccountBookId());
            this.getComputedInventory(returnItems, inventories, inventoryItems, original);
            inventories.forEach(item ->
                    inventoryService.computedInventory(item, false, original.getId(), OperationType.销售退货, null));
            clearReturnCost(returnItems);
        }
    }

    /**
     * 退货成本：优先取原出库明细成本，否则取当前库存平均成本
     */
    private void applyReturnReceiptCost(SalesReturn original, List<SalesReturnItem> returnItems) {
        for (SalesReturnItem line : returnItems) {
            BigDecimal costPrice = null;
            if (line.getOutItemId() != null) {
                SalesOutboundItem outItem = salesOutboundItemRepository.findById(line.getOutItemId()).orElse(null);
                if (outItem != null && outItem.getCostPrice() != null) {
                    costPrice = outItem.getCostPrice();
                }
            }
            if (costPrice == null) {
                Inventory inv = inventoryService.findByWarehouseIdAndProductId(line.getWarehouseId(), line.getProductId());
                costPrice = inv != null && inv.getAverageCost() != null ? inv.getAverageCost() : BigDecimal.ZERO;
            }
            int qty = line.getQuantity() == null ? 0 : line.getQuantity().intValue();
            line.setCostPrice(costPrice);
            line.setCostAmount(costPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_EVEN));
        }
        salesReturnItemRepository.saveAll(returnItems);
    }

    private void createReturnBatches(SalesReturn original, List<SalesReturnItem> returnItems) {
        for (SalesReturnItem item : returnItems) {
            CostingService.ReceiptRequest req = new CostingService.ReceiptRequest();
            req.setProductId(item.getProductId());
            req.setWarehouseId(item.getWarehouseId());
            int qty = item.getQuantity() == null ? 0 : item.getQuantity().intValue();
            req.setQty(qty);
            req.setUnitCost(item.getCostPrice());
            req.setInboundDate(original.getReturnDate());
            req.setOrderId(original.getId());
            req.setOrderType(OperationType.销售退货);
            req.setItemId(item.getId());
            req.setMerchantId(item.getMerchantId());
            req.setAccountBookId(item.getAccountBookId());
            costingService.createReceiptBatch(req);
        }
    }

    private void clearReturnCost(List<SalesReturnItem> returnItems) {
        for (SalesReturnItem line : returnItems) {
            line.setCostPrice(null);
            line.setCostAmount(null);
        }
        salesReturnItemRepository.saveAll(returnItems);
    }

    private void getComputedInventory(List<SalesReturnItem> returnItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, SalesReturn salesReturn) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        returnItems.forEach(returnItem -> {
            BigDecimal quantity = returnItem.getQuantity();
            BigDecimal subtotal = returnItem.getCostAmount();
            if (subtotal == null) {
                subtotal = returnItem.getSubtotal() == null ? BigDecimal.ZERO : returnItem.getSubtotal();
            }
            BigDecimal finalSubtotal = subtotal;
            inventories.stream()
                    .filter(item -> item.getProductId().equals(returnItem.getProductId())
                            && item.getWarehouseId().equals(returnItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal totalCost = item.getTotalCost();
                                BigDecimal added = totalCost.add(finalSubtotal)
                                        .setScale(2, RoundingMode.HALF_EVEN);
                                int parsed = quantity.intValue();
                                currentQuantity += parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setProductId(returnItem.getProductId());
                                inventory.setWarehouseId(returnItem.getWarehouseId());
                                int parsed = returnItem.getQuantity().intValue();
                                inventory.setCurrentQuantity(parsed);
                                inventory.setTotalCost(finalSubtotal);
                                inventory.setMerchantId(returnItem.getMerchantId());
                                inventory.setBaseUnitId(returnItem.getBaseUnitId());
                                inventory.setAccountBookId(returnItem.getAccountBookId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(returnItem, salesReturn, finalSubtotal);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    private InventoryItem getInventoryItem(SalesReturnItem returnItem, SalesReturn salesReturn,
                                           BigDecimal costAmount) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(returnItem.getProductId());
        inventoryItem.setWarehouseId(returnItem.getWarehouseId());
        int parsed = returnItem.getQuantity().intValue();
        inventoryItem.setQuantity(parsed);
        inventoryItem.setBaseUnitId(returnItem.getBaseUnitId());
        inventoryItem.setOperationType(OperationType.销售退货);
        inventoryItem.setOrderId(returnItem.getSalesReturnId());
        inventoryItem.setMerchantId(returnItem.getMerchantId());
        inventoryItem.setBatchNumber(salesReturn.getOrderNo());
        inventoryItem.setAccountBookId(returnItem.getAccountBookId());
        inventoryItem.setCustomerId(salesReturn.getCustomerId());
        inventoryItem.setInventoryDate(Date.from(salesReturn.getReturnDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(returnItem.getCreatedBy());
        inventoryItem.setUnitPrice(returnItem.getCostPrice());
        inventoryItem.setSubtotal(costAmount);
        return inventoryItem;
    }

    public Map<String, BigDecimal> queryTotal(Query query) {
        BigDecimal amount = bqf.selectFrom(qSalesReturn)
                .select(qSalesReturn.refundAmount.sum())
                .where(query.builder).fetchFirst();
        BigDecimal quantity = bqf.selectFrom(qsalesReturnItem)
                .select(qsalesReturnItem.quantity.sum())
                .where(qsalesReturnItem.salesReturnId.in(
                        bqf.selectFrom(qSalesReturn).select(qSalesReturn.id).where(query.builder)
                )).fetchFirst();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", amount);
        result.put("quantity", java.util.Objects.requireNonNullElse(quantity, BigDecimal.ZERO));
        return result;
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSalesReturn.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSalesReturn.accountBookId, accountBookId);
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSalesReturn.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qSalesReturn.orderStatus.eq(state));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qSalesReturn.returnDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qSalesReturn.returnDate.loe(end));
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesReturn.customerId.eq(customerId));
            }
        }
    }
}
