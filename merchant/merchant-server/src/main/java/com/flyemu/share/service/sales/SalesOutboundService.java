package com.flyemu.share.service.sales;

import com.flyemu.share.repository.basic.ProductRepository;
import com.flyemu.share.repository.basic.WarehouseRepository;
import com.flyemu.share.repository.sales.SalesOrderItemRepository;
import com.flyemu.share.repository.sales.SalesOrderRepository;
import com.flyemu.share.repository.sales.SalesOutboundItemRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.repository.sales.SalesReturnRepository;
import com.flyemu.share.common.TenantAware;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.sales.SalesOutboundDto;
import com.flyemu.share.dto.sales.SalesOutboundItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SalesOutboundForm;
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
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesOutboundService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesOutboundItemRepository salesOutboundItemRepository;
    private final CodeSeedService codeSeedService;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final SalesReturnRepository salesReturnRepository;

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryService inventoryService;
    @Autowired
    private CostingService costingService;
    private final PriceRecordService priceRecordService;
    private final CustomerService customerService;

    private static final QAccountBookParameters Q_ACCOUNT_BOOK_PARAMETERS = QAccountBookParameters.accountBookParameters;

    public PageResults<SalesOutboundDto> query(Page page, SalesOutboundService.Query query) {

        long totalSize = bqf.selectFrom(qSalesOutbound)
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesOutbound)
                .select(qSalesOutbound, qCustomer.name, qMerchantUser.name)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOutbound.createdBy))
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOutbound.customerId))
                .where(query.builder)
                .orderBy(qSalesOutbound.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesOutboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOutboundDto salesOutboundDTO = BeanUtil.toBean(tuple.get(qSalesOutbound), SalesOutboundDto.class);
            salesOutboundDTO.setCustomerName(tuple.get(qCustomer.name));
            salesOutboundDTO.setCreatedName(tuple.get(qMerchantUser.name));

            //查询子表
            List<SalesOutboundItem> salesOutboundItemList = bqf.selectFrom(qSalesOutboundItem)
                    .select(qSalesOutboundItem)
                    .where(qSalesOutboundItem.salesOutboundId.eq(salesOutboundDTO.getId()))
                    .fetch();
            List<SalesOutboundItemDto> itemDTOs = new ArrayList<>();
            AtomicReference<Double> totalQuantity = new AtomicReference<>((double) 0L);
            salesOutboundItemList.forEach(item -> {
                SalesOutboundItemDto itemDTO = BeanUtil.toBean(item, SalesOutboundItemDto.class);
                itemDTOs.add(itemDTO);
                Double quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v + quantity);
            });
            salesOutboundDTO.setSalesOutboundItemList(itemDTOs);
            salesOutboundDTO.setTotalQuantity(totalQuantity);

            //封装销售订单编号返回
            List<String> orderNoList = new ArrayList<>();
            for (SalesOutboundItemDto item : itemDTOs) {
                if (item.getSalesOrderId() != null) {
                    SalesOrder salesOrder = salesOrderRepository.findById(item.getSalesOrderId()).orElse(null);
                    if (salesOrder != null) {
                        String orderNo = salesOrder.getOrderNo();
                        item.setSalesOrderNo(orderNo);
                        orderNoList.add(orderNo);
                    }
                }
            }
            //orderNoList 去重
            salesOutboundDTO.setSalesOrderNos(orderNoList.stream().distinct().collect(Collectors.joining(",")));
            dtos.add(salesOutboundDTO);
        });

        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public SalesOutbound save(SalesOutboundForm salesOutboundForm, Long merchantId) {
        SalesOutbound salesOutbound = salesOutboundForm.getSalesOutbound();
        checkoutService.assertEditable(salesOutbound.getMerchantId(), salesOutbound.getAccountBookId(), salesOutbound.getOutboundDate());
        salesOutbound.setMerchantId(merchantId);
        Long id = salesOutbound.getId();
        List<SalesOutboundItem> salesOutboundItemList = salesOutboundForm.getSalesOutboundItemList();

        //查询账套参数：availableInventory 1=允许负库存
        AccountBookParameters accountBookParameters = bqf.selectFrom(Q_ACCOUNT_BOOK_PARAMETERS)
                .where(Q_ACCOUNT_BOOK_PARAMETERS.accountBookId.eq(Math.toIntExact(salesOutbound.getAccountBookId())))
                .fetchOne();
        boolean allowNegative = accountBookParameters != null
                && accountBookParameters.getAvailableInventory() != null
                && accountBookParameters.getAvailableInventory() == 1;
        if (allowNegative) {
            log.info("可用库存允许为负,放行 accountBookParameters:{}", JSONUtil.toJsonStr(salesOutboundItemList));
        } else {
            for (SalesOutboundItem item : salesOutboundItemList) {
                Boolean exist = inventoryService.exist(item.getProductId(), item.getWarehouseId(), salesOutbound.getMerchantId(), salesOutbound.getAccountBookId());
                if (!exist) {
                    Optional<Product> productOptional = productRepository.findById(item.getProductId());
                    Optional<Warehouse> warehouseOptional = warehouseRepository.findById(item.getWarehouseId());

                    String productName = productOptional.map(Product::getName).orElse("未知产品");
                    String warehouseName = warehouseOptional.map(Warehouse::getName).orElse("未知仓库");

                    throw new InvalidContextException(String.format("库存不足：产品「%s」在仓库「%s」中库存不足", productName, warehouseName));
                }
            }
        }

        if (id != null) {
            //查询
            SalesOutbound original = bqf.selectFrom(qSalesOutbound)
                    .where(qSalesOutbound.id.eq(id).and(qSalesOutbound.merchantId.eq(merchantId)))
                    .fetchFirst();
            if (original == null) {
                throw new ServiceException("单据不存在");
            }
            //已审核单据不能修改
            OrderStatus orderStatus = original.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已审核)) {
                throw new InvalidContextException("已审核单据不能修改");
            }
            BeanUtil.copyProperties(salesOutbound, original, CopyOptions.create().ignoreNullValue());
            //修改
            SalesOutbound update = salesOutboundRepository.save(original);
            //保存新关系
            if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                salesOutboundItemList.forEach(item -> {
                    item.setSalesOutboundId(update.getId());
                    item.setAccountBookId(salesOutbound.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setUpdatedAt(LocalDateTime.now());
                });
                //批量修改
                salesOutboundItemRepository.saveAll(salesOutboundItemList);
            }
            //修改关联订单状态
            extractedEdit(salesOutboundItemList);
            return update;
        } else {
            //状态初始化
            salesOutbound.setOrderStatus(OrderStatus.已保存);
            //订单编号
            salesOutbound.setOrderNo(codeSeedService.generateCode(merchantId, salesOutbound.getAccountBookId(), "销售出库单"));
            //保存订单
            SalesOutbound save = salesOutboundRepository.save(salesOutbound);
            if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                salesOutboundItemList.forEach(item -> {
                    item.setSalesOutboundId(save.getId());
                    item.setAccountBookId(salesOutbound.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setCreatedBy(salesOutbound.getCreatedBy());
                    item.setCreatedAt(salesOutbound.getCreatedAt());
                });
                //批量保存
                salesOutboundItemRepository.saveAll(salesOutboundItemList);
            }

            //选择的源单不为空
            List<Long> selectSalesOrderIdList = salesOutboundForm.getSelectSalesOrderIdList();
            if (!CollectionUtils.isEmpty(selectSalesOrderIdList)) {
                //处理订单状态
                extractedEdit(salesOutboundItemList);
            }
            return save;
        }
    }

    private void extractedAdd(List<SalesOutboundItem> salesOutboundItemList) {
        List<Long> orderIdList = new ArrayList<>();
        for (SalesOutboundItem salesOutboundItem : salesOutboundItemList) {
            Long tempId = salesOutboundItem.getTempId();
            if (tempId == null) {
                continue;
            }
            SalesOrderItem salesOrderItemDB = salesOrderItemRepository.getReferenceById(tempId);
            Long salesOrderId = salesOrderItemDB.getSalesOrderId();
            //数据库中的数量
            Double quantity = salesOrderItemDB.getQuantity();
            //数据库中的退货数量
            Double quantityReturn = salesOrderItemDB.getQuantityReturn();
            //页面传递过来的出库数量
            Double quantityOut = salesOutboundItem.getQuantity();
            SalesOrder order = salesOrderRepository.getById(salesOrderId);
            if (quantityOut < (quantity + quantityReturn)) {
                //部分出库 第一此更新，后续有兜底逻辑
                order.setStatus(1);
                salesOrderRepository.save(order);
            }
            //出库数量 出库数量是累加的
            salesOrderItemDB.setQuantityOut(quantityOut + salesOrderItemDB.getQuantityOut());
            salesOrderItemRepository.save(salesOrderItemDB);

            orderIdList.add(salesOrderId);
        }
        //orderIdList 去重
        List<Long> distinct = orderIdList.stream().distinct().toList();
        for (Long salesOrderId : distinct) {
            //兜底逻辑
            SalesOrder salesOrderUpdate = salesOrderRepository.getById(salesOrderId);
            //如果一个订单里面的所有商品都出库完成，将订单状态改成全部出库
            List<SalesOrderItem> salesOrderItemList = jqf.selectFrom(qSalesOrderItem).select(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(salesOrderId)).fetch();
            if (salesOrderItemList.stream().allMatch(item -> (item.getQuantityOut() >= (item.getQuantity() + item.getQuantityReturn())))) {
                salesOrderUpdate.setStatus(2);
                salesOrderRepository.save(salesOrderUpdate);
            }
        }
    }

    private void extractedEdit(List<SalesOutboundItem> salesOutboundItemList) {
        List<Long> orderIdList = new ArrayList<>();
        for (SalesOutboundItem salesOutboundItem : salesOutboundItemList) {
            Long tempId = salesOutboundItem.getTempId();
            if (tempId == null) {
                continue;
            }
            SalesOrderItem salesOrderItemDB = salesOrderItemRepository.getReferenceById(tempId);
            Long salesOrderId = salesOrderItemDB.getSalesOrderId();
            orderIdList.add(salesOrderId);
        }

        List<Long> distinct = orderIdList.stream().distinct().toList();
        for (Long salesOrderId : distinct) {
            SalesOrder salesOrderUpdate = salesOrderRepository.getById(salesOrderId);
            //查询订单id关联的所有出库单商品
            List<SalesOutboundItem> salesOutboundItemListDB = bqf.selectFrom(qSalesOutboundItem)
                    .where(qSalesOutboundItem.salesOrderId.eq(salesOrderId))
                    .fetch();
            //统计所有的出库单商品数量
            Double totalQuantity = salesOutboundItemListDB.stream().mapToDouble(SalesOutboundItem::getQuantity).sum();
            log.info("销售出库单商品数量：{}", totalQuantity);
            if (totalQuantity > 0) {
                salesOrderUpdate.setStatus(1);
            }

            //统计所有销售订单商品数量
            List<SalesOrderItem> salesOrderItemListDB = bqf.selectFrom(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(salesOrderId))
                    .fetch();
            Double totalQuantityOrder = salesOrderItemListDB.stream().mapToDouble(SalesOrderItem::getQuantity).sum();

            log.info("销售订单商品数量：{}", totalQuantityOrder);
            if (totalQuantity >= totalQuantityOrder) {
                log.info("全部出库：salesOrderId:{}", salesOrderId);
                salesOrderUpdate.setStatus(2);
            }
            salesOrderRepository.save(salesOrderUpdate);
        }
    }

    private void recordOutboundPrices(SalesOutbound order) {
        List<SalesOutboundItem> items = salesOutboundItemRepository.findBySalesOutboundId(order.getId());
        if (CollectionUtils.isEmpty(items)) {
            return;
        }
        Date orderDate = order.getOutboundDate() == null ? new Date()
                : Date.from(order.getOutboundDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        for (SalesOutboundItem item : items) {
            PriceRecord priceRecord = new PriceRecord();
            priceRecord.setOrderId(order.getId());
            priceRecord.setOrderDate(orderDate);
            priceRecord.setUnitPrice(item.getUnitPrice());
            priceRecord.setBaseUnitId(item.getBaseUnitId());
            priceRecord.setProductId(item.getProductId());
            priceRecord.setMerchantId(order.getMerchantId());
            priceRecord.setAccountBookId(order.getAccountBookId());
            priceRecord.setCustomerId(order.getCustomerId());
            priceRecord.setQuantity(item.getQuantity());
            priceRecord.setPriceSource(PriceSource.最近销售价格);
            priceRecord.setPriceType(PriceType.最近销售价格);
            priceRecordService.appendTradePrice(priceRecord);
        }
    }

    private void removeOutboundPrices(SalesOutbound order) {
        priceRecordService.removeByOrder(
                order.getId(),
                PriceType.最近销售价格,
                PriceSource.最近销售价格,
                order.getMerchantId(),
                order.getAccountBookId()
        );
    }

    @Transactional
    public void delete(Long salesOutboundId, Long merchantId, Long accountBookId) {

        SalesOutbound original = salesOutboundRepository.getById(salesOutboundId);
        //已审核单据不能删除
        OrderStatus orderStatus = original.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已审核)) {
            throw new InvalidContextException("已审核单据不能删除");
        }

        //已关联销售退货单不能删除
        Long returnOrderId = original.getReturnOrderId();
        if (returnOrderId != null) {
            Optional<SalesReturn> salesReturnOptional = salesReturnRepository.findById(returnOrderId);
            salesReturnOptional.ifPresent(salesReturn -> {
                throw new InvalidContextException("已关联销售退货单不能删除");
            });
        }
        List<Long> salesOrderIds = bqf.select(qSalesOutboundItem.salesOrderId)
                .from(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOutboundId.eq(salesOutboundId))
                .distinct()
                .fetch();

        if (!salesOrderIds.isEmpty()) {
            jqf.update(QSalesOrder.salesOrder)
                    .set(QSalesOrder.salesOrder.status, 0)
                    .where(QSalesOrder.salesOrder.id.in(salesOrderIds))
                    .execute();
        }
        jqf.delete(qSalesOutbound)
                .where(qSalesOutbound.id.eq(salesOutboundId).and(qSalesOutbound.merchantId.eq(merchantId)).and(qSalesOutbound.accountBookId.eq(accountBookId)))
                .execute();

        //删除出库单商品
        jqf.delete(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOutboundId.eq(salesOutboundId).and(qSalesOutboundItem.merchantId.eq(merchantId)).and(qSalesOutboundItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesOutbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesOutbound).where(qSalesOutbound.merchantId.eq(merchantId).and(qSalesOutbound.accountBookId.eq(accountBookId))).fetch();
    }

    public SalesOutboundDto load(Long merchantId, Long orderId) {
        SalesOutbound salesOutbound = bqf.selectFrom(qSalesOutbound)
                .where(qSalesOutbound.merchantId.eq(merchantId).and(qSalesOutbound.id.eq(orderId)))
                .fetchFirst();
        if (salesOutbound == null) {
            throw new ServiceException("单据不存在");
        }
        SalesOutboundDto dto = BeanUtil.toBean(salesOutbound, SalesOutboundDto.class);
        List<Tuple> fetch = jqf.selectFrom(qSalesOutboundItem)
                .select(qSalesOutboundItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOutboundItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOutboundItem.baseUnitId))
                .where(qSalesOutboundItem.salesOutboundId.eq(orderId)
                        .and(qSalesOutboundItem.merchantId.eq(merchantId)))
                .orderBy(qSalesOutboundItem.id.asc()).fetch();
        List<SalesOutboundItemDto> salesOutboundItemDTOList = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesOutboundItemDto salesOutboundItemDTO = BeanUtil.toBean(tuple.get(qSalesOutboundItem), SalesOutboundItemDto.class);
            salesOutboundItemDTO.setProductName(tuple.get(qProduct.name));
            salesOutboundItemDTO.setProductCode(tuple.get(qProduct.code));
            salesOutboundItemDTO.setUnitName(tuple.get(qUnit.name));

            Long salesOrderId = salesOutboundItemDTO.getSalesOrderId();
            if (salesOrderId != null) {
                SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId).orElse(null);
                if (salesOrder != null) {
                    salesOutboundItemDTO.setSalesOrderNo(salesOrder.getOrderNo());
                }
            }
            salesOutboundItemDTOList.add(salesOutboundItemDTO);
        });
        dto.setSalesOutboundItemList(salesOutboundItemDTOList);
        return dto;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择单据");
        }
        List<SalesOutbound> salesOutboundList = bqf.selectFrom(qSalesOutbound)
                .where(qSalesOutbound.merchantId.eq(merchantId).and(qSalesOutbound.id.in(ids)))
                .fetch();
        if (salesOutboundList.isEmpty()) {
            throw new ServiceException("未找到数据~");
        }
        salesOutboundList.forEach(order -> {
            if (OrderStatus.已保存.equals(state)) {
                boolean hasPaymentOrVerification = checkHasPaymentOrVerification(order.getId());
                if (hasPaymentOrVerification) {
                    log.error("存在付款单或核销单，无法反审核-----orderId:{}", order.getId());
                    throw new ServiceException("存在付款单或核销单，无法反审核");
                }
                Long returnOrderId = order.getReturnOrderId();
                if (returnOrderId != null) {
                    Optional<SalesReturn> salesReturnOptional = salesReturnRepository.findById(returnOrderId);
                    salesReturnOptional.ifPresent(salesReturn -> {
                        throw new ServiceException("已关联销售退货单不能反审核");
                    });
                }
                removeOutboundPrices(order);
            } else if (OrderStatus.已审核.equals(state)) {
                if (OrderStatus.已保存.equals(order.getOrderStatus())) {
                    recordOutboundPrices(order);
                }
            }
            this.updateCustomerBalanceAndRecordFlow(order, state);
            order.setOrderStatus(state);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(adminId);
        });

        salesOutboundRepository.saveAll(salesOutboundList);
        salesOutboundList.forEach(this::salesOutboundToInventory);
    }

    private boolean checkHasPaymentOrVerification(Long inboundId) {
        QOrderReceiptItem orderReceiptItem = QOrderReceiptItem.orderReceiptItem;
        QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
        QVerification qVerification = QVerification.verification;

        long paymentCount = jqf.select(orderReceiptItem.id.count())
                .from(orderReceiptItem)
                .where(orderReceiptItem.salesOrderId.eq(inboundId)
                        .and(orderReceiptItem.businessType.eq(1)))
                .fetchOne();

        long verificationCount = jqf.select(qVerificationItem.id.count())
                .from(qVerificationItem)
                .leftJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId))
                .where(qVerificationItem.businessId.eq(inboundId.intValue()).and(qVerification.type.eq(1))
                        .and(qVerificationItem.businessType.eq(1)))
                .fetchOne();
        long paymentTotal = Optional.of(paymentCount).orElse(0L);
        long verificationTotal = Optional.of(verificationCount).orElse(0L);
        return paymentTotal > 0 || verificationTotal > 0;
    }

    private void updateCustomerBalanceAndRecordFlow(SalesOutbound salesOutbound, OrderStatus targetStatus) {
        Customer customer = customerService.findById(salesOutbound.getCustomerId());
        BigDecimal amount = salesOutbound.getFinalAmount();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }

        CustomerFlow flow = getCustomerFlow(salesOutbound, targetStatus);

        if (targetStatus == OrderStatus.已审核) {
            customer.setBalance(customer.getBalance().add(amount));
        } else {
            if (!OrderStatus.已审核.equals(salesOutbound.getOrderStatus())) {
                throw new ServiceException("只有已审核的单据才能反审核");
            }
            customer.setBalance(customer.getBalance().subtract(amount));
        }

        flow.setBalanceReceivables(customer.getBalance());
        customerService.updateTheBalance(customer, flow);
    }

    private CustomerFlow getCustomerFlow(SalesOutbound salesOutbound, OrderStatus targetStatus) {
        CustomerFlow flow = new CustomerFlow();
        flow.setCustomerId(salesOutbound.getCustomerId());
        flow.setBusinessId(salesOutbound.getId());
        flow.setBusinessNo(salesOutbound.getOrderNo());
        flow.setBusinessDate(salesOutbound.getOutboundDate());

        BigDecimal finalAmount = salesOutbound.getFinalAmount();

        flow.setRemarks(targetStatus == OrderStatus.已审核 ? "销售出库单审核通过" : "销售出库单反审核");

        if (targetStatus == OrderStatus.已审核) {
            flow.setCustomerFlowType(CustomerFlow.CustomerFlowType.销售出库单);
            flow.setSalesAmount(finalAmount);
            flow.setReceivableAmount(finalAmount);
            flow.setPreferentialAmount(salesOutbound.getDiscountAmount());
        } else {
            flow.setCustomerFlowType(CustomerFlow.CustomerFlowType.反审核_销售出库单);
            flow.setSalesAmount(finalAmount.negate());
            flow.setReceivableAmount(finalAmount.negate());
            if (salesOutbound.getDiscountAmount()!=null){
                flow.setPreferentialAmount(salesOutbound.getDiscountAmount().negate());
            }
        }
        flow.setBalanceReceivables(BigDecimal.ZERO);
        flow.setAccountBookId(salesOutbound.getAccountBookId());
        flow.setMerchantId(salesOutbound.getMerchantId());
        flow.setCreatedBy(salesOutbound.getApprovedBy());
        flow.setCreatedAt(LocalDateTime.now());
        return flow;
    }

    private void salesOutboundToInventory(SalesOutbound original) {
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        List<SalesOutboundItem> outboundItems = jqf.selectFrom(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOutboundId.eq(original.getId())).fetch();
        if (OrderStatus.已审核.equals(original.getOrderStatus())) {
            applyIssueCost(original, outboundItems);
            this.getComputedInventory(outboundItems, inventories, inventoryItems, original);
            inventories.forEach(item ->
                    inventoryService.computedInventory(item, false, original.getId(), OperationType.销售出库, inventoryItems));
        } else {
            // 反审：先按明细已落成本回补库存余额，再回补批次
            this.getComputedInventory(outboundItems, inventories, inventoryItems, original);
            inventories.forEach(item ->
                    inventoryService.computedInventory(item, true, original.getId(), OperationType.销售出库, null));
            costingService.reverseIssue(original.getId(), OperationType.销售出库,
                    original.getMerchantId(), original.getAccountBookId());
            clearIssueCost(outboundItems);
        }
    }

    /**
     * 审核出库：按成本法扣批次并回写明细成本
     */
    private void applyIssueCost(SalesOutbound original, List<SalesOutboundItem> outboundItems) {
        for (SalesOutboundItem line : outboundItems) {
            int qty = line.getQuantity() == null ? 0 : (int) Double.parseDouble(line.getQuantity().toString());
            CostingService.IssueRequest req = new CostingService.IssueRequest();
            req.setProductId(line.getProductId());
            req.setWarehouseId(line.getWarehouseId());
            req.setQty(qty);
            req.setOrderId(original.getId());
            req.setOrderType(OperationType.销售出库);
            req.setItemId(line.getId());
            req.setMerchantId(original.getMerchantId());
            req.setAccountBookId(original.getAccountBookId());
            CostingService.IssueResult result = costingService.issue(req);
            line.setCostPrice(result.getCostPrice());
            line.setCostAmount(result.getCostAmount());
        }
        salesOutboundItemRepository.saveAll(outboundItems);
    }

    private void clearIssueCost(List<SalesOutboundItem> outboundItems) {
        for (SalesOutboundItem line : outboundItems) {
            line.setCostPrice(null);
            line.setCostAmount(null);
        }
        salesOutboundItemRepository.saveAll(outboundItems);
    }

    private void getComputedInventory(List<SalesOutboundItem> outboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, SalesOutbound salesOutbound) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        outboundItems.forEach(outboundItem -> {
            Double quantity = outboundItem.getQuantity();
            Long productId = outboundItem.getProductId();
            // 优先使用审核写入的成本；反审时回补同样金额
            BigDecimal subtotal = outboundItem.getCostAmount();
            if (subtotal == null) {
                Inventory inv = inventoryService.findByWarehouseIdAndProductId(
                        outboundItem.getWarehouseId(), productId);
                BigDecimal avg = inv != null && inv.getAverageCost() != null ? inv.getAverageCost() : BigDecimal.ZERO;
                subtotal = avg.multiply(BigDecimal.valueOf(quantity == null ? 0D : quantity))
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            BigDecimal finalSubtotal = subtotal;
            inventories.stream()
                    .filter(item -> item.getProductId().equals(productId)
                            && item.getWarehouseId().equals(outboundItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal totalCost = item.getTotalCost();
                                BigDecimal added = totalCost.add(finalSubtotal)
                                        .setScale(2, RoundingMode.HALF_EVEN);
                                double parsed = Double.parseDouble(quantity.toString());
                                currentQuantity += (int) parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setProductId(outboundItem.getProductId());
                                inventory.setWarehouseId(outboundItem.getWarehouseId());
                                double parsed = Double.parseDouble(outboundItem.getQuantity().toString());
                                inventory.setCurrentQuantity((int) parsed);
                                inventory.setTotalCost(finalSubtotal);
                                inventory.setMerchantId(outboundItem.getMerchantId());
                                inventory.setBaseUnitId(outboundItem.getBaseUnitId());
                                inventory.setAccountBookId(outboundItem.getAccountBookId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(outboundItem, salesOutbound, finalSubtotal);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    private InventoryItem getInventoryItem(SalesOutboundItem outboundItem, SalesOutbound salesOutbound,
                                           BigDecimal costAmount) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(outboundItem.getProductId());
        inventoryItem.setWarehouseId(outboundItem.getWarehouseId());
        double parsed = Double.parseDouble(outboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(outboundItem.getBaseUnitId());
        inventoryItem.setOperationType(OperationType.销售出库);
        inventoryItem.setOrderId(outboundItem.getSalesOutboundId());
        inventoryItem.setMerchantId(outboundItem.getMerchantId());
        inventoryItem.setBatchNumber(salesOutbound.getOrderNo());
        inventoryItem.setAccountBookId(outboundItem.getAccountBookId());
        inventoryItem.setCustomerId(salesOutbound.getCustomerId());
        inventoryItem.setInventoryDate(Date.from(salesOutbound.getOutboundDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(outboundItem.getCreatedBy());
        // 流水记录成本，不再写入销售单价
        inventoryItem.setUnitPrice(outboundItem.getCostPrice());
        inventoryItem.setSubtotal(costAmount);
        return inventoryItem;
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qSalesOutbound)
                .select(qSalesOutbound.finalAmount.sum())
                .where(query.builder).fetchFirst();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSalesOutbound.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSalesOutbound.accountBookId, accountBookId);
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSalesOutbound.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qSalesOutbound.orderStatus.eq(state));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qSalesOutbound.outboundDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qSalesOutbound.outboundDate.loe(end));
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
            }
        }

        //查询未退货订单
        public void setQueryUnReturnOrder(Integer queryUnReturnOrder) {
            if (queryUnReturnOrder != null && queryUnReturnOrder == 1) {
                builder.and(qSalesOutbound.returnOrderId.isNull());
            }
        }
    }
}
