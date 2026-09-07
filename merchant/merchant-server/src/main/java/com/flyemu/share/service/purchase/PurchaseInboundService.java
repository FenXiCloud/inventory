package com.flyemu.share.service.purchase;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.UnitConvert;
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
import com.flyemu.share.dto.PurchaseInboundImportVo;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.purchase.PurchaseInboundDto;
import com.flyemu.share.dto.purchase.PurchaseInboundItemDto;
import com.flyemu.share.dto.purchase.PurchaseOrderDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.basic.PriceRecord;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.fund.QOrderPaymentItem;
import com.flyemu.share.entity.fund.QSettlement;
import com.flyemu.share.entity.fund.QSettlementItem;
import com.flyemu.share.service.fund.SettlementService;
import com.flyemu.share.entity.fund.QVerification;
import com.flyemu.share.entity.fund.QVerificationItem;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOrderItem;
import com.flyemu.share.entity.sales.SalesOrderItem;
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
import com.flyemu.share.service.basic.ProductService;
import com.flyemu.share.service.basic.ProductAuxiliaryUnitService;
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
    private final static QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;
    private final static QPurchaseInboundReturnConnection qConnection = QPurchaseInboundReturnConnection.purchaseInboundReturnConnection;
    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundItemRepository inboundItemRepository;

    private final PriceRecordService priceRecordService;
    private final CodeSeedService codeSeedService;
    private final SupplierService supplierService;

    private final InventoryService inventoryService;
    private final CostingService costingService;
    private final SettlementService settlementService;
    private final ProductService productService;
    private final ProductAuxiliaryUnitService productAuxiliaryUnitService;
    // 采购入库单列表,把是数据拼成一条完整的记录,分页返回给前端
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
            Set<String> orderNoSet = new HashSet<>(bqf.selectFrom(qPurchaseOrder).select(qPurchaseOrder.orderNo)
                    .where(qPurchaseOrder.purchaseInboundId.eq(dto.getId())).fetch());
            // 新模型：订单分批入库，入库行带来源订单引用（订单头无 purchaseInboundId）
            orderNoSet.addAll(bqf.select(qPurchaseOrder.orderNo)
                    .from(qPurchaseInboundItem)
                    .innerJoin(qPurchaseOrder).on(qPurchaseOrder.id.eq(qPurchaseInboundItem.purchaseOrderId))
                    .where(qPurchaseInboundItem.purchaseInboundId.eq(dto.getId())
                            .and(qPurchaseInboundItem.purchaseOrderId.isNotNull()))
                    .distinct().fetch());

            List<String> returnOrderNos = bqf.selectFrom(qConnection)
                    .select(qPurchaseInbound.orderNo)
                    .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qConnection.purchaseInboundId))
                    .where(qConnection.purchaseReturnId.eq(dto.getId())).fetch();

            if (CollUtil.isNotEmpty(orderNoSet)) {
                dto.setPurchaseOrderNos(String.join(",", orderNoSet));
            }

            if (CollUtil.isNotEmpty(returnOrderNos)) {
                dto.setPurchaseReturnOrderNo(String.join(",", returnOrderNos));
            }
            // 结算状态
            QSettlement qS = QSettlement.settlement;
            QSettlementItem qSI = QSettlementItem.settlementItem;
            String status = jqf.select(qS.orderStatus.stringValue())
                    .from(qSI)
                    .innerJoin(qS).on(qS.id.eq(qSI.settlementId))
                    .where(qSI.businessId.eq(dto.getId())
                            .and(qSI.businessCategory.eq("INVENTORY"))
                            .and(qSI.businessType.eq("采购入库单")))
                    .fetchFirst();
            dto.setSettlementStatus(status);
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }
// 采购入库单统计
    public Map<String, BigDecimal> queryTotal(Query query) {
        Tuple tuple = bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound.finalAmount.sum(), qPurchaseInbound.secondarySum.sum())
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder).fetchOne();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", tuple.get(0, BigDecimal.class));
        result.put("quantity", java.util.Objects.requireNonNullElse(tuple.get(1, BigDecimal.class), BigDecimal.ZERO));
        return result;
    }

    @Transactional
    public PurchaseInbound save(PurchaseInboundForm purchaseInboundForm, Long merchantId) {
        PurchaseInbound purchaseInbound = purchaseInboundForm.getPurchaseInbound();
        checkoutService.assertEditable(purchaseInbound.getMerchantId(), purchaseInbound.getAccountBookId(), purchaseInbound.getInboundDate());
        // 分批入库硬校验：来源订单行累计入库不得超过该订单行数量（编辑时排除本单自身已入库量）
        validateInboundQuantities(purchaseInboundForm.getPurchaseInboundItemList(),
                purchaseInbound.getId() != null ? purchaseInbound.getId() : null);
        if (purchaseInbound.getId() != null) {
            PurchaseInbound original = purchaseInboundRepository.getById(purchaseInbound.getId());
            Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能更新~");
            BeanUtil.copyProperties(purchaseInbound, original, CopyOptions.create().ignoreNullValue());
            jqf.delete(QPurchaseInboundItem.purchaseInboundItem)
                    .where(QPurchaseInboundItem.purchaseInboundItem.purchaseInboundId.eq(purchaseInbound.getId()))
                    .execute();
            Set<Long> ids = new HashSet<>();
            BigDecimal secondarySum = BigDecimal.ZERO;
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                if (d.getProductId() == null) {
                    throw new ServiceException("明细产品不能为空~");
                }
                if (d.getWarehouseId() == null) {
                    throw new ServiceException("明细仓库不能为空~");
                }
                // 自动分配货位
                assignLocation(d, merchantId, purchaseInbound.getAccountBookId());
                //服务端权威换算：基本数量 = 采购数量 × 换算率；基本单价 = 采购单价 ÷ 换算率
                d.setQuantity(UnitConvert.toBaseQty(d.getSecondaryQuantity(), d.getConversionRate()));
                d.setUnitPrice(UnitConvert.unitPrice(d.getSecondaryPrice(), d.getConversionRate()));
                // 金额字段以用户录入为准，仅当缺省时补算小计
                if (d.getSubtotal() == null && d.getSecondaryPrice() != null) {
                    d.setSubtotal(d.getSecondaryPrice().multiply(nvl(d.getSecondaryQuantity()))
                            .subtract(nvl(d.getDiscountAmount())).setScale(2, RoundingMode.HALF_UP));
                }
                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
                d.setReturnQuantity(d.getSecondaryQuantity());
                secondarySum = secondarySum.add(d.getSecondaryQuantity());
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            original.setSecondarySum(secondarySum);
            original.setReturnSum(secondarySum);
            return purchaseInboundRepository.save(original);
        } else {

            purchaseInbound.setOrderNo(codeSeedService.generateCode(merchantId, purchaseInbound.getAccountBookId(), "采购入库单"));
            purchaseInbound.setOrderStatus(OrderStatus.已保存);
            BigDecimal secondarySum = purchaseInboundForm.getPurchaseInboundItemList()
                    .stream()
                    .map(PurchaseInboundItem::getSecondaryQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            purchaseInbound.setSecondarySum(secondarySum);
            purchaseInbound.setReturnSum(secondarySum);
            purchaseInbound = purchaseInboundRepository.save(purchaseInbound);

            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                if (d.getProductId() == null) {
                    throw new ServiceException("明细产品不能为空~");
                }
                if (d.getWarehouseId() == null) {
                    throw new ServiceException("明细仓库不能为空~");
                }
                // 自动分配货位
                assignLocation(d, merchantId, purchaseInbound.getAccountBookId());
                //服务端权威换算：基本数量 = 采购数量 × 换算率；基本单价 = 采购单价 ÷ 换算率
                d.setQuantity(UnitConvert.toBaseQty(d.getSecondaryQuantity(), d.getConversionRate()));
                d.setUnitPrice(UnitConvert.unitPrice(d.getSecondaryPrice(), d.getConversionRate()));
                // 金额字段以用户录入为准，仅当缺省时补算小计
                if (d.getSubtotal() == null && d.getSecondaryPrice() != null) {
                    d.setSubtotal(d.getSecondaryPrice().multiply(nvl(d.getSecondaryQuantity()))
                            .subtract(nvl(d.getDiscountAmount())).setScale(2, RoundingMode.HALF_UP));
                }
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
                d.setReturnQuantity(d.getSecondaryQuantity());
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            return purchaseInbound;
        }
    }

    /**
     * 分批入库硬校验：任一来源订单行（purchaseOrderItemId）本次入库量（含同单其他行引用）不得超过
     * 该订单行剩余可入库数量（订单行基本数量 − 其他入库单已引用入库量）。
     */
    private void validateInboundQuantities(List<PurchaseInboundItem> items, Long excludeInboundId) {
        if (CollUtil.isEmpty(items)) {
            return;
        }
        Set<Long> lineIds = new HashSet<>();
        Map<Long, BigDecimal> sumBaseByLine = new HashMap<>();
        for (PurchaseInboundItem d : items) {
            if (d.getPurchaseOrderItemId() == null) {
                continue;
            }
            lineIds.add(d.getPurchaseOrderItemId());
            // 基本数量一律服务端按 采购数量×换算率 计算（不信任客户端基本数量；同一订单行拆多行在此求和校验）
            BigDecimal base = UnitConvert.toBaseQty(d.getSecondaryQuantity(), d.getConversionRate());
            sumBaseByLine.merge(d.getPurchaseOrderItemId(), base, BigDecimal::add);
        }
        if (lineIds.isEmpty()) {
            return;
        }
        // 订单行基本数量 / 商品id / 商品名称 / 基本单位名称
        Map<Long, BigDecimal> lineQty = new HashMap<>();
        Map<Long, Long> lineProduct = new HashMap<>();
        Map<Long, String> lineUnitName = new HashMap<>();
        List<Tuple> lineRows = bqf.selectFrom(qPurchaseOrderItem)
                .select(qPurchaseOrderItem.id, qPurchaseOrderItem.productId, qPurchaseOrderItem.quantity, qUnit.name)
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseOrderItem.baseUnitId))
                .where(qPurchaseOrderItem.id.in(lineIds)).fetch();
        Set<Long> productIds = new HashSet<>();
        for (Tuple row : lineRows) {
            lineQty.put(row.get(qPurchaseOrderItem.id), row.get(qPurchaseOrderItem.quantity));
            lineProduct.put(row.get(qPurchaseOrderItem.id), row.get(qPurchaseOrderItem.productId));
            lineUnitName.put(row.get(qPurchaseOrderItem.id), row.get(qUnit.name));
            productIds.add(row.get(qPurchaseOrderItem.productId));
        }
        Map<Long, String> productName = new HashMap<>();
        if (CollUtil.isNotEmpty(productIds)) {
            for (Tuple r : bqf.selectFrom(qProduct).select(qProduct.id, qProduct.name)
                    .where(qProduct.id.in(productIds)).fetch()) {
                productName.put(r.get(qProduct.id), r.get(qProduct.name));
            }
        }
        // 其他入库单已引用入库量（编辑时排除本单自身，避免把将被删除的旧行计入）
        Map<Long, BigDecimal> consumed = consumedByOrderItemIds(lineIds, excludeInboundId);
        for (Tuple row : lineRows) {
            Long lineId = row.get(qPurchaseOrderItem.id);
            BigDecimal remain = nvl(lineQty.get(lineId)).subtract(nvl(consumed.get(lineId)));
            BigDecimal sumBase = nvl(sumBaseByLine.get(lineId));
            if (sumBase.compareTo(remain) > 0) {
                String name = productName.get(lineProduct.get(lineId));
                String unitName = lineUnitName.get(lineId);
                throw new ServiceException("商品「" + (name == null ? lineId : name) + "」本次入库数量超出订单剩余可入库数量（剩余 "
                        + remain.stripTrailingZeros().toPlainString() + (unitName == null ? "" : " " + unitName)
                        + "），请调整数量或另开普通采购入库单");
            }
        }
    }

    /**
     * 汇总指定采购订单行已被入库的基本数量（仅统计带来源引用 purchaseOrderItemId 的行）
     */
    private Map<Long, BigDecimal> consumedByOrderItemIds(Collection<Long> lineIds, Long excludeInboundId) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (CollUtil.isEmpty(lineIds)) {
            return map;
        }
        BooleanBuilder cond = new BooleanBuilder(qPurchaseInboundItem.purchaseOrderItemId.in(lineIds)
                .and(qPurchaseInboundItem.purchaseOrderItemId.isNotNull()));
        if (excludeInboundId != null) {
            cond.and(qPurchaseInboundItem.purchaseInboundId.ne(excludeInboundId));
        }
        for (Tuple row : bqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInboundItem.purchaseOrderItemId, qPurchaseInboundItem.quantity)
                .where(cond).fetch()) {
            map.merge(row.get(qPurchaseInboundItem.purchaseOrderItemId), nvl(row.get(qPurchaseInboundItem.quantity)), BigDecimal::add);
        }
        return map;
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 自动分配货位
     * 根据入库类型（整件/零货）自动分配到对应货位
     */
    private void assignLocation(PurchaseInboundItem item, Long merchantId, Long accountBookId) {
        // 如果已经指定了货位，不再自动分配
        if (item.getLocationId() != null) {
            return;
        }

        // 获取商品信息
        Product product = productService.loadById(item.getProductId(), merchantId);
        if (product == null) {
            return;
        }

        // 判断是整件入库还是零货入库
        Integer isCase = item.getIsCase();
        if (isCase == null) {
            // 默认为零货入库
            isCase = 0;
        }

        // 根据入库类型分配货位
        if (isCase == 1) {
            // 整件入库 -> 分配到整件货位
            item.setLocationId(product.getDefaultWholeLocationId());
        } else {
            // 零货入库 -> 分配到零货货位
            item.setLocationId(product.getDefaultZeroLocationId());
        }

        log.debug("自动分配货位：商品={}, 入库类型={}, 货位ID={}",
                product.getName(), isCase == 1 ? "整件" : "零货", item.getLocationId());
    }
    //采购入库单价格记录
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
    //最近采购价格
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

        // 结账日期校验：已结账的单据不能删除
        checkoutService.assertEditable(original.getMerchantId(), original.getAccountBookId(), original.getInboundDate());

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
    //
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
    //获取采购退货列表
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
    //审核商品逻辑
    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        List<PurchaseInbound> orders = bqf.selectFrom(qPurchaseInbound)
                .where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.id.in(ids)))
                .fetch();

        Assert.isFalse(CollUtil.isEmpty(orders), "未找到数据~");

        // 结账日期校验：已结账的单据不能审核/反审核
        for (PurchaseInbound order : orders) {
            checkoutService.assertEditable(order.getMerchantId(), order.getAccountBookId(), order.getInboundDate());
        }

        List<Long> setIds = new ArrayList<>();
        //判断状态
        if (OrderStatus.已审核.equals(state)) {
            for (PurchaseInbound order : orders) {
                if (!OrderStatus.已保存.equals(order.getOrderStatus())) {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                    continue;
                }

                inboundSupplierFlows(adminId, order);
                setIds.add(order.getId());
                recordInboundPrices(order);
                // 自动生成结算单
                String supplierName = bqf.selectFrom(qSupplier).select(qSupplier.name)
                        .where(qSupplier.id.eq(order.getSupplierId())).fetchOne();
                settlementService.createFromOrder(order.getMerchantId(), order.getAccountBookId(),
                        2, order.getSupplierId(), supplierName != null ? supplierName : "",
                        order.getOrderNo(), order.getFinalAmount(), order.getId(), "采购入库单",
                        order.getInboundDate());
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
                supplier.setBalance(supplier.getBalance().subtract(finalAmount));
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
                // 删除随审核自动生成的结算单（明细 + 空主表），避免残留"未平账"空单
                settlementService.removeAutoSettlement(order.getMerchantId(), order.getAccountBookId(),
                        order.getId(), "采购入库单");
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

            // 回写关联销售订单的采购状态
            if (OrderStatus.已审核.equals(state)) {
                for (PurchaseInbound order : orders) {
                    if (setIds.contains(order.getId()) && order.getSourceSalesOrderId() != null) {
                        updateSalesOrderPurchaseStatus(order.getSourceSalesOrderId(), merchantId);
                    }
                }
            } else if (OrderStatus.已保存.equals(state)) {
                // 反审核时也要回写
                for (PurchaseInbound order : orders) {
                    if (order.getSourceSalesOrderId() != null) {
                        updateSalesOrderPurchaseStatus(order.getSourceSalesOrderId(), merchantId);
                    }
                }
            }
        }
    }
        //更新供应商应付余额 + 生成供应商资金流水台账
    private void inboundSupplierFlows(Long adminId, PurchaseInbound order) {
        Supplier supplier = supplierService.selectByPrimaryKey(order.getSupplierId());
        BigDecimal finalAmount = order.getFinalAmount();
        supplier.setBalance(supplier.getBalance().add(finalAmount));
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
    //判断是否能反审核
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
    //采购入库单转库存
    private void purchaseInboundToInventory(OrderStatus state, List<Long> setIds) {
        setIds.forEach(id -> {
            purchaseInboundRepository.findById(id).ifPresent(purchaseInbound -> {
                List<Inventory> inventories = new ArrayList<>();
                List<InventoryItem> inventoryItems = new ArrayList<>();
                List<PurchaseInboundItem> inboundItems = inboundItemRepository.findByPurchaseInboundId(purchaseInbound.getId());
                for (PurchaseInboundItem item : inboundItems) {
                    if (item.getProductId() == null) {
                        throw new ServiceException("单据「" + purchaseInbound.getOrderNo() + "」存在未选择产品的明细，无法审核");
                    }
                    if (item.getWarehouseId() == null) {
                        throw new ServiceException("单据「" + purchaseInbound.getOrderNo() + "」存在未选择仓库的明细，无法审核");
                    }
                }
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
//    采购入库审核时，生成成本批次
    private void createCostBatches(PurchaseInbound purchaseInbound, List<PurchaseInboundItem> inboundItems) {
        for (PurchaseInboundItem item : inboundItems) {
            CostingService.ReceiptRequest req = new CostingService.ReceiptRequest();
            req.setProductId(item.getProductId());
            req.setWarehouseId(item.getWarehouseId());
            int qty = item.getQuantity() == null ? 0 : item.getQuantity().intValue();
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
    //采购入库的时候,批量计算更新库存主表,生成库存变动流水

    private void getComputedInventory(List<PurchaseInboundItem> inboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, PurchaseInbound purchaseInbound) {
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        inboundItems.forEach(purchaseInboundItem -> {
            BigDecimal subtotal = purchaseInboundItem.getSubtotal();
            BigDecimal quantity = purchaseInboundItem.getQuantity();
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
                                currentQuantity += quantity.intValue();
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setWarehouseId(purchaseInboundItem.getWarehouseId());
                                inventory.setProductId(purchaseInboundItem.getProductId());
                                int parsed = purchaseInboundItem.getQuantity().intValue();
                                inventory.setCurrentQuantity(parsed);
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
        int parsed = purchaseInboundItem.getQuantity().intValue();
        inventoryItem.setQuantity(parsed);
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
    //入库单详情页面
    public Dict load(Long merchantId, Long orderId) {
        Tuple fetchFirst = jqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.id.eq(orderId))).fetchFirst();

        QUnit qUnit1 = new QUnit("id");

        PurchaseOrderDto orderDto = BeanUtil.toBean(fetchFirst.get(qPurchaseInbound), PurchaseOrderDto.class);
        orderDto.setSupplierName(fetchFirst.get(qSupplier.name));

        // 查询来源销售订单编号
        if (orderDto.getSourceSalesOrderId() != null) {
            String sourceOrderNo = bqf.selectFrom(qSalesOrder)
                    .select(qSalesOrder.orderNo)
                    .where(qSalesOrder.id.eq(orderDto.getSourceSalesOrderId()))
                    .fetchFirst();
            orderDto.setSourceSalesOrderNo(sourceOrderNo);
        }
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
        // 回填商品可用单位列表（基本单位在前，unitPrice=该行基本单价），编辑/复制入库单时"采购单位"下拉可切换
        Set<Long> productIds = new HashSet<>();
        for (PurchaseInboundItemDto dto : collect) {
            if (dto.getProductId() != null) {
                productIds.add(dto.getProductId());
            }
        }
        Map<Long, List<AuxiliaryUnitPrice>> unitMap = productAuxiliaryUnitService.loadAuxUnits(productIds, merchantId);
        for (PurchaseInboundItemDto dto : collect) {
            dto.setAuxiliaryUnitPrices(ProductAuxiliaryUnitService.withBase(
                    dto.getBaseUnitId(), dto.getBaseUnitName(), dto.getUnitPrice(), unitMap.get(dto.getProductId())));
        }
        return Dict.create().set("purchaseInbound", orderDto).set("purchaseInboundItemList", collect);
    }
    // 采购入库单
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();
        // 订单状态
        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qPurchaseInbound.orderStatus.eq(state));
            }
        }
        // 订单编号
        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qPurchaseInbound.orderNo.contains(filter));
            }
        }
        // 开始时间
        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseInbound.inboundDate.goe(start));
            }
        }
        // 结束时间
        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseInbound.inboundDate.loe(end));
            }
        }
        // 商户Id
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

    /**
     * 更新关联销售订单的采购状态
     */
    private void updateSalesOrderPurchaseStatus(Long salesOrderId, Long merchantId) {
        // 查询该销售订单的所有已审核采购入库单的明细数量（按商品汇总）
        List<PurchaseInboundItem> allInboundItems = bqf.selectFrom(qPurchaseInboundItem)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .where(qPurchaseInbound.sourceSalesOrderId.eq(salesOrderId)
                        .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseInboundItem.merchantId.eq(merchantId)))
                .fetch();

        Map<Long, BigDecimal> purchasedByProduct = new java.util.HashMap<>();
        for (PurchaseInboundItem item : allInboundItems) {
            purchasedByProduct.merge(item.getProductId(), item.getSecondaryQuantity(), BigDecimal::add);
        }

        // 查询销售订单明细
        List<SalesOrderItem> orderItems = bqf.selectFrom(qSalesOrderItem)
                .where(qSalesOrderItem.salesOrderId.eq(salesOrderId).and(qSalesOrderItem.merchantId.eq(merchantId)))
                .fetch();

        if (orderItems.isEmpty()) return;

        boolean allFull = true;
        boolean anyPurchased = false;

        for (SalesOrderItem orderItem : orderItems) {
            BigDecimal orderQty = orderItem.getQuantity() != null ? orderItem.getQuantity() : BigDecimal.ZERO;
            BigDecimal purchasedQty = purchasedByProduct.getOrDefault(orderItem.getProductId(), BigDecimal.ZERO);
            if (purchasedQty.compareTo(BigDecimal.ZERO) > 0) {
                anyPurchased = true;
            }
            if (purchasedQty.compareTo(orderQty) < 0) {
                allFull = false;
            }
        }

        Integer newStatus;
        if (allFull && anyPurchased) {
            newStatus = 2;
        } else if (anyPurchased) {
            newStatus = 1;
        } else {
            newStatus = 0;
        }

        jqf.update(qSalesOrder)
                .set(qSalesOrder.purchaseStatus, newStatus)
                .where(qSalesOrder.id.eq(salesOrderId))
                .execute();
    }

    @Transactional
    public void importData(List<PurchaseInboundImportVo> rows, Long merchantId, Long accountBookId, Long adminId) {
        for (int i = 0; i < rows.size(); i++) {
            PurchaseInboundImportVo row = rows.get(i);
            int excelRow = i + 2;
            if (StrUtil.isEmpty(row.getSupplierName())) {
                throw new ServiceException("第" + excelRow + "行：供应商名称不能为空");
            }
            if (StrUtil.isEmpty(row.getProductCode()) && StrUtil.isEmpty(row.getProductName())) {
                throw new ServiceException("第" + excelRow + "行：产品编码或产品名称不能为空");
            }
            if (row.getQuantity() == null || row.getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("第" + excelRow + "行：数量必须大于0");
            }
            if (row.getUnitPrice() == null) {
                throw new ServiceException("第" + excelRow + "行：单价不能为空");
            }
        }

        // 按单据编号分组（空编号 = 每行独立订单）
        Map<String, List<PurchaseInboundImportVo>> groups = new LinkedHashMap<>();
        for (PurchaseInboundImportVo row : rows) {
            String key = StrUtil.isNotEmpty(row.getOrderNo()) ? row.getOrderNo() : "ROW_" + System.nanoTime();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<PurchaseInboundImportVo>> entry : groups.entrySet()) {
            List<PurchaseInboundImportVo> group = entry.getValue();
            PurchaseInboundImportVo first = group.get(0);

            Supplier supplier;
            if (StrUtil.isNotEmpty(first.getSupplierCode())) {
                supplier = bqf.selectFrom(qSupplier).where(qSupplier.code.eq(first.getSupplierCode()).and(qSupplier.merchantId.eq(merchantId))).fetchFirst();
            } else {
                supplier = bqf.selectFrom(qSupplier).where(qSupplier.name.eq(first.getSupplierName()).and(qSupplier.merchantId.eq(merchantId))).fetchFirst();
            }
            if (supplier == null) throw new ServiceException("供应商「" + (StrUtil.isNotEmpty(first.getSupplierCode()) ? first.getSupplierCode() : first.getSupplierName()) + "」不存在");

            LocalDate inboundDate;
            try { inboundDate = LocalDate.parse(first.getInboundDate()); } catch (Exception e) { throw new ServiceException("入库日期格式错误：" + first.getInboundDate()); }

            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalDiscountRate = first.getDiscountRate() != null ? first.getDiscountRate() : BigDecimal.ZERO;
            List<PurchaseInboundItem> items = new ArrayList<>();

            for (PurchaseInboundImportVo row : group) {
                Product product = null;
                if (StrUtil.isNotEmpty(row.getProductCode())) {
                    product = bqf.selectFrom(qProduct).where(qProduct.code.eq(row.getProductCode()).and(qProduct.merchantId.eq(merchantId))).fetchFirst();
                }
                if (product == null && StrUtil.isNotEmpty(row.getProductName())) {
                    product = bqf.selectFrom(qProduct).where(qProduct.name.eq(row.getProductName()).and(qProduct.merchantId.eq(merchantId))).fetchFirst();
                }
                if (product == null) throw new ServiceException("产品「" + (StrUtil.isNotEmpty(row.getProductCode()) ? row.getProductCode() : row.getProductName()) + "」不存在");

                Warehouse warehouse = null;
                if (StrUtil.isNotEmpty(row.getWarehouseName())) {
                    warehouse = bqf.selectFrom(qWarehouse).where(qWarehouse.name.eq(row.getWarehouseName()).and(qWarehouse.merchantId.eq(merchantId))).fetchFirst();
                }
                if (warehouse == null) warehouse = bqf.selectFrom(qWarehouse).where(qWarehouse.merchantId.eq(merchantId).and(qWarehouse.systemDefault.isTrue())).fetchFirst();

                BigDecimal qty = row.getQuantity(); BigDecimal price = row.getUnitPrice();
                BigDecimal dr = row.getDiscountRate() != null ? row.getDiscountRate() : BigDecimal.ZERO;
                BigDecimal st = qty.multiply(price);
                BigDecimal da = st.multiply(dr).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

                PurchaseInboundItem item = new PurchaseInboundItem();
                item.setProductId(product.getId()); item.setBaseUnitId(product.getUnitId());
                item.setQuantity(qty); item.setSecondaryQuantity(qty);
                item.setSecondaryUnitId(product.getUnitId()); item.setConversionRate(BigDecimal.ONE);
                item.setUnitPrice(price); item.setSecondaryPrice(price);
                item.setDiscountRate(dr); item.setDiscountAmount(da);
                item.setSubtotal(st.subtract(da)); item.setWarehouseId(warehouse != null ? warehouse.getId() : null);
                item.setCreatedBy(adminId); item.setCreatedAt(LocalDateTime.now());
                item.setMerchantId(merchantId); item.setAccountBookId(accountBookId);
                item.setReturnQuantity(BigDecimal.ZERO); items.add(item);
                totalSubtotal = totalSubtotal.add(st);
            }

            BigDecimal totalDiscountAmount = totalSubtotal.multiply(totalDiscountRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            PurchaseInbound inbound = new PurchaseInbound();
            inbound.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "采购入库单"));
            inbound.setSupplierId(supplier.getId()); inbound.setInboundDate(inboundDate);
            inbound.setTotalAmount(totalSubtotal); inbound.setDiscountRate(totalDiscountRate);
            inbound.setDiscountAmount(totalDiscountAmount); inbound.setFinalAmount(totalSubtotal.subtract(totalDiscountAmount));
            inbound.setRemarks(first.getRemarks()); inbound.setOrderStatus(OrderStatus.已保存);
            inbound.setCreatedBy(adminId); inbound.setCreatedAt(LocalDateTime.now());
            inbound.setMerchantId(merchantId); inbound.setAccountBookId(accountBookId);
            inbound.setVerifiedAmount(BigDecimal.ZERO); inbound.setPaymentAmount(BigDecimal.ZERO);
            PurchaseInbound saved = purchaseInboundRepository.save(inbound);

            for (PurchaseInboundItem item : items) {
                item.setPurchaseInboundId(saved.getId());
                inboundItemRepository.save(item);
            }
        }
    }
}
