package com.flyemu.share.service.purchase;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.PurchaseInboundDto;
import com.flyemu.share.dto.purchase.PurchaseInboundItemDto;
import com.flyemu.share.dto.purchase.PurchaseOrderDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.basic.PriceRecord;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.fund.QOrderPaymentItem;
import com.flyemu.share.entity.fund.QVerification;
import com.flyemu.share.entity.fund.QVerificationItem;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.PurchaseInboundForm;
import com.flyemu.share.repository.purchase.PurchaseInboundItemRepository;
import com.flyemu.share.repository.purchase.PurchaseInboundRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.basic.SupplierService;
import com.flyemu.share.service.inventory.CostingService;
import com.flyemu.share.service.inventory.InventoryService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseInboundService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;
    private final static QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;
    private final static QPurchaseInboundReturnConnection qConnection = QPurchaseInboundReturnConnection.purchaseInboundReturnConnection;

    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundItemRepository inboundItemRepository;

    private final PriceRecordService priceRecordService;
    private final CodeSeedService codeSeedService;
    private final SupplierService supplierService;

    private final InventoryService inventoryService;
    private final CostingService costingService;

    public PageResults<PurchaseInboundDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder).orderBy(qPurchaseInbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseInboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseInboundDto dto = BeanUtil.toBean(tuple.get(qPurchaseInbound), PurchaseInboundDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dto.setPurchaseReturnOrderNo(tuple.get(qPurchaseReturn.orderNo));
            List<String> orderNos = bqf.selectFrom(qPurchaseOrder).select(qPurchaseOrder.orderNo).where(qPurchaseOrder.purchaseInboundId.eq(dto.getId())).fetch();

            List<String> returnOrderNos = bqf.selectFrom(qConnection)
                    .select(qPurchaseInbound.orderNo)
                    .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qConnection.purchaseInboundId))
                    .where(qConnection.purchaseReturnId.eq(dto.getId())).fetch();

            if (CollUtil.isNotEmpty(orderNos)) {
                dto.setPurchaseOrderNos(String.join(",", orderNos));
            }

            if (CollUtil.isNotEmpty(returnOrderNos)) {
                dto.setPurchaseReturnOrderNo(String.join(",", returnOrderNos));
            }
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound.finalAmount.sum())
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder).fetchFirst();
    }

    @Transactional
    public PurchaseInbound save(PurchaseInboundForm purchaseInboundForm, Long merchantId) {
        PurchaseInbound purchaseInbound = purchaseInboundForm.getPurchaseInbound();
        checkoutService.assertEditable(purchaseInbound.getMerchantId(), purchaseInbound.getAccountBookId(), purchaseInbound.getInboundDate());
        if (purchaseInbound.getId() != null) {
            PurchaseInbound original = purchaseInboundRepository.getById(purchaseInbound.getId());
            Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能更新~");
            BeanUtil.copyProperties(purchaseInbound, original, CopyOptions.create().ignoreNullValue());
            jqf.delete(QPurchaseInboundItem.purchaseInboundItem)
                    .where(QPurchaseInboundItem.purchaseInboundItem.purchaseInboundId.eq(purchaseInbound.getId()))
                    .execute();
            Set<Long> ids = new HashSet<>();
            Double secondarySum = 0.0;
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));
                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
                d.setReturnQuantity(d.getSecondaryQuantity());
                secondarySum += d.getSecondaryQuantity();
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            original.setSecondarySum(secondarySum);
            original.setReturnSum(secondarySum);
            return purchaseInboundRepository.save(original);
        } else {

            purchaseInbound.setOrderNo(codeSeedService.generateCode(merchantId, purchaseInbound.getAccountBookId(), "采购入库单"));
            purchaseInbound.setOrderStatus(OrderStatus.已保存);
            Double secondarySum = purchaseInboundForm.getPurchaseInboundItemList()
                    .stream()
                    .map(PurchaseInboundItem::getSecondaryQuantity)
                    .reduce(0.0, Double::sum);
            purchaseInbound.setSecondarySum(secondarySum);
            purchaseInbound.setReturnSum(secondarySum);
            purchaseInbound = purchaseInboundRepository.save(purchaseInbound);

            if (CollUtil.isNotEmpty(purchaseInboundForm.getOrderIds())) {
                jqf.update(qPurchaseOrder)
                        .set(qPurchaseOrder.purchaseInboundId, purchaseInbound.getId())
                        .where(qPurchaseOrder.id.in(purchaseInboundForm.getOrderIds()))
                        .execute();
            }
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
                d.setReturnQuantity(d.getSecondaryQuantity());
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            return purchaseInbound;
        }
    }

    private void recordInboundPrices(PurchaseInbound order) {
        List<PurchaseInboundItem> items = inboundItemRepository.findByPurchaseInboundId(order.getId());
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Date orderDate = order.getInboundDate() == null ? new Date()
                : Date.from(order.getInboundDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        for (PurchaseInboundItem item : items) {
            PriceRecord priceRecord = new PriceRecord();
            priceRecord.setUnitPrice(item.getUnitPrice());
            priceRecord.setBaseUnitId(item.getBaseUnitId());
            priceRecord.setProductId(item.getProductId());
            priceRecord.setMerchantId(item.getMerchantId());
            priceRecord.setSupplierId(order.getSupplierId());
            priceRecord.setAccountBookId(order.getAccountBookId());
            priceRecord.setOrderId(order.getId());
            priceRecord.setOrderDate(orderDate);
            priceRecord.setQuantity(item.getQuantity());
            priceRecord.setPriceSource(PriceSource.最近采购价格);
            priceRecord.setPriceType(PriceType.最近采购价格);
            priceRecordService.appendTradePrice(priceRecord);
        }
    }

    private void removeInboundPrices(PurchaseInbound order) {
        priceRecordService.removeByOrder(
                order.getId(),
                PriceType.最近采购价格,
                PriceSource.最近采购价格,
                order.getMerchantId(),
                order.getAccountBookId()
        );
    }

    @Transactional
    public void delete(Long purchaseInboundId, Long merchantId, Long accountBookId) {

        PurchaseInbound original = purchaseInboundRepository.getById(purchaseInboundId);

        Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能删除~");

        Assert.isFalse(bqf.selectFrom(qConnection)
                .where(qConnection.purchaseInboundId.eq(purchaseInboundId))
                .fetchCount() > 0, "已关联退货单不能删除~");
        jqf.update(QPurchaseOrder.purchaseOrder)
                .set(QPurchaseOrder.purchaseOrder.purchaseInboundId, (Long) null)
                .where(QPurchaseOrder.purchaseOrder.purchaseInboundId.eq(purchaseInboundId))
                .execute();
        jqf.delete(qPurchaseInboundItem)
                .where(qPurchaseInboundItem.purchaseInboundId.eq(purchaseInboundId).and(qPurchaseInboundItem.accountBookId.eq(accountBookId)).and(qPurchaseInboundItem.merchantId.eq(merchantId)))
                .execute();
        jqf.delete(qPurchaseInbound)
                .where(qPurchaseInbound.id.eq(purchaseInboundId).and(qPurchaseInbound.merchantId.eq(merchantId)).and(qPurchaseInbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseInbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseInbound).where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.accountBookId.eq(accountBookId))).fetch();
    }

    public PageResults<PurchaseInboundDto> listToReturn(Page page, PurchaseInboundService.Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder.and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseInbound.returnSum.gt(0))).orderBy(qPurchaseInbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseInboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseInboundDto dto = BeanUtil.toBean(tuple.get(qPurchaseInbound), PurchaseInboundDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public List<PurchaseInboundItemDto> loadToReturn(List<Long> orderIds, Long merchantId, Long supplierId) {
        QUnit qUnit1 = new QUnit("id");

        return bqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInboundItem, qProduct.code, qProduct.name, qWarehouse.name, qPurchaseInbound.orderNo,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name, qProductCategory.name, qProduct.specification)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId).and(qPurchaseInbound.merchantId.eq(merchantId)))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseInboundItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseInboundItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(qPurchaseInboundItem.purchaseInboundId.in(orderIds).and(qPurchaseInboundItem.merchantId.eq(merchantId))
                        .and(qPurchaseInboundItem.returnQuantity.gt(0))
                        .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核)))
                .orderBy(qPurchaseInboundItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseInboundItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseInboundItem), PurchaseInboundItemDto.class);
                    dto.setId(null);
                    dto.setPurchaseInboundOrderNo(tuple.get(qPurchaseInbound.orderNo));
                    dto.setSecondaryQuantity(dto.getReturnQuantity());
                    dto.setPurchaseInboundItemId(tuple.get(qPurchaseInboundItem).getId());
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setSpec(tuple.get(qProduct.specification));
                    dto.setCategoryName(tuple.get(qProductCategory.name));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setWarehouseName(tuple.get(qWarehouse.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    list.add(dto);
                }, List::addAll);
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        List<PurchaseInbound> orders = bqf.selectFrom(qPurchaseInbound)
                .where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.id.in(ids)))
                .fetch();

        Assert.isFalse(CollUtil.isEmpty(orders), "未找到数据~");
        List<Long> setIds = new ArrayList<>();

        if (OrderStatus.已审核.equals(state)) {
            for (PurchaseInbound order : orders) {
                if (!OrderStatus.已保存.equals(order.getOrderStatus())) {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                    continue;
                }

                inboundSupplierFlows(adminId, order);
                setIds.add(order.getId());
                recordInboundPrices(order);
            }
        } else if (OrderStatus.已保存.equals(state)) {
            for (PurchaseInbound order : orders) {
                if (!OrderStatus.已审核.equals(order.getOrderStatus())) {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                    continue;
                }

                if (bqf.selectFrom(qConnection).where(qConnection.purchaseInboundId.eq(order.getId())).fetchCount() > 0) {
                    throw new ServiceException("存在已关联的退货单，无法反审核");
                }

                boolean hasPaymentOrVerification = checkHasPaymentOrVerification(order.getId());
                if (hasPaymentOrVerification) {
                    log.error("存在付款单或核销单，无法反审核-----orderId:{}", order.getId());
                    throw new ServiceException("存在付款单或核销单，无法反审核");
                }
                BigDecimal finalAmount = order.getFinalAmount();
                Supplier supplier = supplierService.selectByPrimaryKey(order.getSupplierId());
                supplier.setBalance(supplier.getBalance().add(finalAmount));
                SupplierFlow flow = new SupplierFlow();
                flow.setSupplierId(order.getSupplierId());
                flow.setBusinessId(order.getId());
                flow.setBusinessNo(order.getOrderNo());
                flow.setSupplierFlowType(SupplierFlow.SupplierFlowType.反审核_采购入库单);
                flow.setPurchaseAmount(finalAmount.negate());
                flow.setCopeWithAmount(finalAmount.negate());
                if (order.getDiscountAmount() != null) {
                    flow.setPreferentialAmount(order.getDiscountAmount().negate());
                }
                flow.setBalancePayable(supplier.getBalance());
                flow.setAccountBookId(order.getAccountBookId());
                flow.setMerchantId(order.getMerchantId());
                flow.setCreatedBy(adminId);
                flow.setCreatedAt(LocalDateTime.now());
                flow.setRemarks("采购入库单反审核");
                flow.setBusinessDate(order.getInboundDate());
                supplierService.updateTheBalance(supplier, flow);
                setIds.add(order.getId());
                removeInboundPrices(order);
            }
        }

        if (CollUtil.isNotEmpty(setIds)) {
            jqf.update(qPurchaseInbound)
                    .set(qPurchaseInbound.orderStatus, state)
                    .set(qPurchaseInbound.approvedAt, LocalDateTime.now())
                    .set(qPurchaseInbound.approvedBy, adminId)
                    .where(qPurchaseInbound.id.in(setIds))
                    .execute();

            this.purchaseInboundToInventory(state, setIds);
        }
    }

    private void inboundSupplierFlows(Long adminId, PurchaseInbound order) {
        Supplier supplier = supplierService.selectByPrimaryKey(order.getSupplierId());
        BigDecimal finalAmount = order.getFinalAmount();
        supplier.setBalance(supplier.getBalance().subtract(finalAmount));
        SupplierFlow flow = new SupplierFlow();
        flow.setSupplierId(order.getSupplierId());
        flow.setBusinessId(order.getId());
        flow.setBusinessNo(order.getOrderNo());
        flow.setSupplierFlowType(SupplierFlow.SupplierFlowType.采购入库单);
        flow.setPurchaseAmount(finalAmount);
        if (order.getDiscountAmount() != null) {
            flow.setPreferentialAmount(order.getDiscountAmount().negate());
        }
        flow.setCopeWithAmount(finalAmount);
        flow.setBalancePayable(supplier.getBalance());
        flow.setAccountBookId(order.getAccountBookId());
        flow.setMerchantId(order.getMerchantId());
        flow.setCreatedBy(adminId);
        flow.setCreatedAt(LocalDateTime.now());
        flow.setRemarks("采购入库单审核通过");
        flow.setBusinessDate(order.getInboundDate());
        supplierService.updateTheBalance(supplier, flow);
    }

    private boolean checkHasPaymentOrVerification(Long inboundId) {
        QOrderPaymentItem qOrderPaymentItem = QOrderPaymentItem.orderPaymentItem;
        QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
        QVerification qVerification = QVerification.verification;

        long paymentCount = jqf.select(qOrderPaymentItem.id.count())
                .from(qOrderPaymentItem)
                .where(qOrderPaymentItem.businessId.eq(inboundId)
                        .and(qOrderPaymentItem.businessType.eq(1)))
                .fetchOne();

        long verificationCount = jqf.select(qVerificationItem.id.count())
                .from(qVerificationItem)
                .leftJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId))
                .where(qVerificationItem.businessId.eq(inboundId.intValue()).and(qVerification.type.eq(2))
                        .and(qVerificationItem.businessType.eq(1)))
                .fetchOne();
        long paymentTotal = Optional.of(paymentCount).orElse(0L);
        long verificationTotal = Optional.of(verificationCount).orElse(0L);
        return paymentTotal > 0 || verificationTotal > 0;
    }

    private void purchaseInboundToInventory(OrderStatus state, List<Long> setIds) {
        setIds.forEach(id -> {
            purchaseInboundRepository.findById(id).ifPresent(purchaseInbound -> {
                List<Inventory> inventories = new ArrayList<>();
                List<InventoryItem> inventoryItems = new ArrayList<>();
                List<PurchaseInboundItem> inboundItems = inboundItemRepository.findByPurchaseInboundId(purchaseInbound.getId());
                //处理库存
                this.getComputedInventory(inboundItems, inventories, inventoryItems, purchaseInbound);
                if (OrderStatus.已审核.equals(state)) {
                    // 加库存
                    inventories.forEach(item ->
                            inventoryService.computedInventory(item, true, id, OperationType.采购入库, inventoryItems));
                    createCostBatches(purchaseInbound, inboundItems);
                } else {
                    // 先校验并删除批次，再减库存
                    costingService.reverseReceipt(id, OperationType.采购入库,
                            purchaseInbound.getMerchantId(), purchaseInbound.getAccountBookId());
                    inventories.forEach(item ->
                            inventoryService.computedInventory(item, false, id, OperationType.采购入库, null));
                }
            });
        });
    }

    private void createCostBatches(PurchaseInbound purchaseInbound, List<PurchaseInboundItem> inboundItems) {
        for (PurchaseInboundItem item : inboundItems) {
            CostingService.ReceiptRequest req = new CostingService.ReceiptRequest();
            req.setProductId(item.getProductId());
            req.setWarehouseId(item.getWarehouseId());
            int qty = item.getQuantity() == null ? 0 : (int) Double.parseDouble(item.getQuantity().toString());
            req.setQty(qty);
            req.setUnitCost(item.getUnitPrice());
            req.setInboundDate(purchaseInbound.getInboundDate());
            req.setOrderId(purchaseInbound.getId());
            req.setOrderType(OperationType.采购入库);
            req.setItemId(item.getId());
            req.setSupplierId(purchaseInbound.getSupplierId());
            req.setMerchantId(item.getMerchantId());
            req.setAccountBookId(item.getAccountBookId());
            costingService.createReceiptBatch(req);
        }
    }

    private void getComputedInventory(List<PurchaseInboundItem> inboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, PurchaseInbound purchaseInbound) {
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        inboundItems.forEach(purchaseInboundItem -> {
            BigDecimal subtotal = purchaseInboundItem.getSubtotal();
            Double quantity = purchaseInboundItem.getQuantity();
            inventories.stream()
                    .filter(item -> item.getProductId().equals(purchaseInboundItem.getProductId())
                            && item.getWarehouseId().equals(purchaseInboundItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                BigDecimal totalCost = item.getTotalCost();
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal added = totalCost.add(subtotal)
                                        .setScale(2, RoundingMode.HALF_EVEN);
                                double parsed = Double.parseDouble(quantity.toString());
                                currentQuantity += (int) parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setWarehouseId(purchaseInboundItem.getWarehouseId());
                                inventory.setProductId(purchaseInboundItem.getProductId());
                                double parsed = Double.parseDouble(purchaseInboundItem.getQuantity().toString());
                                inventory.setCurrentQuantity((int) parsed);
                                inventory.setTotalCost(purchaseInboundItem.getSubtotal());
                                inventory.setMerchantId(purchaseInboundItem.getMerchantId());
                                inventory.setAccountBookId(purchaseInboundItem.getAccountBookId());
                                inventory.setBaseUnitId(purchaseInboundItem.getBaseUnitId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(purchaseInboundItem, purchaseInbound);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param purchaseInboundItem 入库明细
     * @param purchaseInbound
     * @return inventoryItem
     */
    private InventoryItem getInventoryItem(PurchaseInboundItem purchaseInboundItem, PurchaseInbound purchaseInbound) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setWarehouseId(purchaseInboundItem.getWarehouseId());
        inventoryItem.setProductId(purchaseInboundItem.getProductId());
        double parsed = Double.parseDouble(purchaseInboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(purchaseInboundItem.getBaseUnitId());
        inventoryItem.setSupplierId(purchaseInbound.getSupplierId());
        inventoryItem.setOperationType(OperationType.采购入库);
        inventoryItem.setBaseUnitId(purchaseInboundItem.getBaseUnitId());
        inventoryItem.setOrderId(purchaseInboundItem.getPurchaseInboundId());
        inventoryItem.setBatchNumber(purchaseInbound.getOrderNo());
        inventoryItem.setInventoryDate(Date.from(purchaseInbound.getInboundDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        inventoryItem.setMerchantId(purchaseInboundItem.getMerchantId());
        inventoryItem.setAccountBookId(purchaseInboundItem.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(purchaseInboundItem.getCreatedBy());
        inventoryItem.setUnitPrice(purchaseInboundItem.getUnitPrice());
        inventoryItem.setSubtotal(purchaseInboundItem.getSubtotal());
        return inventoryItem;
    }

    public Dict load(Long merchantId, Long orderId) {
        Tuple fetchFirst = jqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.id.eq(orderId))).fetchFirst();

        QUnit qUnit1 = new QUnit("id");

        PurchaseOrderDto orderDto = BeanUtil.toBean(fetchFirst.get(qPurchaseInbound), PurchaseOrderDto.class);
        orderDto.setSupplierName(fetchFirst.get(qSupplier.name));
        ArrayList<PurchaseInboundItemDto> collect = jqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInboundItem, qProduct.code, qProduct.name, qWarehouse.name, qProductCategory.name,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name, qProduct.specification)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseInboundItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseInboundItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .where(qPurchaseInboundItem.purchaseInboundId.eq(orderId).and(qPurchaseInboundItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseInboundItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseInboundItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseInboundItem), PurchaseInboundItemDto.class);
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setWarehouseName(tuple.get(qWarehouse.name));
                    dto.setSpec(tuple.get(qProduct.specification));
                    dto.setCategoryName(tuple.get(qProductCategory.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    list.add(dto);
                }, List::addAll);
        return Dict.create().set("purchaseInbound", orderDto).set("purchaseInboundItemList", collect);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qPurchaseInbound.orderStatus.eq(state));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qPurchaseInbound.orderNo.contains(filter));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseInbound.inboundDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseInbound.inboundDate.loe(end));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPurchaseInbound.merchantId, merchantId);
        }

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qPurchaseInbound.supplierId.eq(supplierId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPurchaseInbound.accountBookId, accountBookId);
        }
    }
}
