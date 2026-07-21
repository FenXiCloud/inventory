package com.flyemu.share.service.sales;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesReturnDTO;
import com.flyemu.share.dto.SalesReturnItemDTO;
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
import com.flyemu.share.repository.*;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.inventory.InventoryService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/**
 * @功能描述: 销售出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReturnService extends AbsService {

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
    private final CustomerService customerService;

    public PageResults<SalesReturnDTO> query(Page page, SalesReturnService.Query query) {
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

        List<SalesReturnDTO> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesReturnDTO salesReturnDTO = BeanUtil.toBean(tuple.get(qSalesReturn), SalesReturnDTO.class);
            salesReturnDTO.setCustomerName(tuple.get(qCustomer.name));
            salesReturnDTO.setCreatedName(tuple.get(qMerchantUser.name));
            //查询子表
            List<SalesReturnItem> salesReturnItemList = bqf.selectFrom(qsalesReturnItem)
                    .select(qsalesReturnItem)
                    .where(qsalesReturnItem.salesReturnId.eq(salesReturnDTO.getId()))
                    .fetch();
            List<SalesReturnItemDTO> itemDTOs = new ArrayList<>();
            AtomicReference<Double> totalQuantity = new AtomicReference<>((double) 0L);
            salesReturnItemList.forEach(item -> {
                SalesReturnItemDTO itemDTO = BeanUtil.toBean(item, SalesReturnItemDTO.class);
                itemDTOs.add(itemDTO);
                Double quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v + quantity);
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
            Double quantity = salesOutboundItem.getQuantity();
            //退货单数量
            Double quantity1 = item.getQuantity();
            if (quantity1 > quantity) {
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

    public SalesReturnDTO load(Long merchantId, Long orderId) {
        SalesReturn salesReturn = bqf.selectFrom(qSalesReturn)
                .where(qSalesReturn.merchantId.eq(merchantId).and(qSalesReturn.id.eq(orderId)))
                .fetchFirst();
        if (salesReturn == null) {
            throw new ServiceException("单据不存在");
        }
        SalesReturnDTO dto = BeanUtil.toBean(salesReturn, SalesReturnDTO.class);
        List<Tuple> fetch = jqf.selectFrom(qsalesReturnItem)
                .select(qsalesReturnItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qsalesReturnItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qsalesReturnItem.baseUnitId))
                .where(qsalesReturnItem.salesReturnId.eq(orderId)
                        .and(qsalesReturnItem.merchantId.eq(merchantId)))
                .orderBy(qsalesReturnItem.id.asc()).fetch();
        List<SalesReturnItemDTO> salesReturnItemDTOList = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesReturnItemDTO salesReturnItemDTO = BeanUtil.toBean(tuple.get(qsalesReturnItem), SalesReturnItemDTO.class);
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
            customer.setBalance(customer.getBalance().add(amount));
        } else {
            customer.setBalance(customer.getBalance().subtract(amount));
        }

        flow.setBalanceReceivables(customer.getBalance());
        customerService.updateTheBalance(customer,flow);
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
        List<SalesReturnItem> returnItems = jqf.selectFrom(qsalesReturnItem).where(qsalesReturnItem.salesReturnId.eq(original.getId())).fetch();
        //处理库存
        this.getComputedInventory(returnItems, inventories, inventoryItems, original);
        inventories.forEach(item -> {
            if (OrderStatus.已审核.equals(original.getOrderStatus())) {
                // 加库存
                inventoryService.computedInventory(item, false, original.getId(), OperationType.销售退货, inventoryItems);
            } else {
                // 减库存
                inventoryService.computedInventory(item, true, original.getId(), OperationType.销售退货, null);
            }
        });
    }

    private void getComputedInventory(List<SalesReturnItem> returnItems, List<Inventory> inventories, List<InventoryItem> inventoryItems, SalesReturn salesReturn) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        returnItems.forEach(otherOutboundItem -> {
            Double quantity = otherOutboundItem.getQuantity();
            BigDecimal subtotal = otherOutboundItem.getSubtotal();
            inventories.stream()
                    .filter(item -> item.getProductId().equals(otherOutboundItem.getProductId())
                            && item.getWarehouseId().equals(otherOutboundItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal totalCost = item.getTotalCost();
                                BigDecimal added = totalCost.add(subtotal)
                                        .setScale(2, RoundingMode.HALF_EVEN);
                                double parsed = Double.parseDouble(quantity.toString());
                                currentQuantity += (int) parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setProductId(otherOutboundItem.getProductId());
                                inventory.setWarehouseId(otherOutboundItem.getWarehouseId());
                                double parsed = Double.parseDouble(otherOutboundItem.getQuantity().toString());
                                inventory.setCurrentQuantity((int) parsed);
                                inventory.setTotalCost(otherOutboundItem.getSubtotal());
                                inventory.setMerchantId(otherOutboundItem.getMerchantId());
                                inventory.setBaseUnitId(otherOutboundItem.getBaseUnitId());
                                inventory.setAccountBookId(otherOutboundItem.getAccountBookId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(otherOutboundItem, salesReturn);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    private InventoryItem getInventoryItem(SalesReturnItem otherOutboundItem, SalesReturn salesReturn) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(otherOutboundItem.getProductId());
        inventoryItem.setWarehouseId(otherOutboundItem.getWarehouseId());
        double parsed = Double.parseDouble(otherOutboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOperationType(OperationType.销售退货);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOrderId(otherOutboundItem.getSalesReturnId());
        inventoryItem.setMerchantId(otherOutboundItem.getMerchantId());
        inventoryItem.setBatchNumber(salesReturn.getOrderNo());
        inventoryItem.setAccountBookId(otherOutboundItem.getAccountBookId());
        inventoryItem.setCustomerId(salesReturn.getCustomerId());
        inventoryItem.setInventoryDate(Date.from(salesReturn.getReturnDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(otherOutboundItem.getCreatedBy());
        inventoryItem.setUnitPrice(otherOutboundItem.getUnitPrice());
        inventoryItem.setSubtotal(otherOutboundItem.getSubtotal());
        return inventoryItem;
    }


    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qSalesReturn)
                .select(qSalesReturn.finalAmount.sum())
                .where(query.builder).fetchFirst();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesReturn.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesReturn.accountBookId.eq(accountBookId));
            }
        }

        public void setFilter(String filter) {
            if (StringUtils.isNotBlank(filter)) {
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
