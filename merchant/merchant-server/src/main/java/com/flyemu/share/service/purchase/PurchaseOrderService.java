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
import com.flyemu.share.dto.purchase.PurchaseInboundItemDto;
import com.flyemu.share.dto.purchase.PurchaseOrderDto;
import com.flyemu.share.dto.purchase.PurchaseOrderItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.PurchaseOrderForm;
import com.flyemu.share.dto.PurchaseOrderImportVo;
import com.flyemu.share.repository.purchase.PurchaseOrderItemRepository;
import com.flyemu.share.repository.purchase.PurchaseOrderRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.PriceRecordService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseOrderService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QUnit qUnit = QUnit.unit;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final CodeSeedService codeSeedService;
    private final PriceRecordService priceRecordService;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;

    public PageResults<PurchaseOrderDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder, qSupplier.name, qMerchantUser.name, qPurchaseInbound.orderNo)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseOrder.purchaseInboundId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseOrder.createdBy))
                .where(query.builder).orderBy(qPurchaseOrder.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseOrderDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseOrderDto dto = BeanUtil.toBean(tuple.get(qPurchaseOrder), PurchaseOrderDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setPurchaseInboundOrderNo(tuple.get(qPurchaseInbound.orderNo));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public PageResults<PurchaseOrderDto> queryToInBound(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseOrder.createdBy))
                .where(query.builder.and(qPurchaseOrder.purchaseInboundId.isNull())).orderBy(qPurchaseOrder.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseOrderDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseOrderDto dto = BeanUtil.toBean(tuple.get(qPurchaseOrder), PurchaseOrderDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public Map<String, BigDecimal> queryTotal(Query query) {
        Tuple tuple = bqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder.finalAmount.sum(), qPurchaseOrder.secondarySum.sum())
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseOrder.createdBy))
                .where(query.builder).fetchOne();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", tuple.get(0, BigDecimal.class));
        result.put("quantity", java.util.Objects.requireNonNullElse(tuple.get(1, BigDecimal.class), BigDecimal.ZERO));
        return result;
    }

    public List<PurchaseInboundItemDto> loadToInbound(List<Long> orderIds, Long merchantId, Long supplierId) {
        QUnit qUnit1 = new QUnit("id");

        return bqf.selectFrom(qPurchaseOrderItem)
                .select(qPurchaseOrderItem, qProduct.code, qProduct.name, qWarehouse.name, qProductCategory.name, qProduct.specification,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseOrderItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qPurchaseOrder).on(qPurchaseOrder.id.eq(qPurchaseOrderItem.purchaseOrderId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseOrderItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseOrderItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseOrderItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(qPurchaseOrderItem.purchaseOrderId.in(orderIds).and(qPurchaseOrderItem.merchantId.eq(merchantId))
                        .and(qPurchaseOrder.orderStatus.eq(OrderStatus.已审核)).and(qPurchaseOrder.purchaseInboundId.isNull()))
                .orderBy(qPurchaseOrderItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseInboundItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseOrderItem), PurchaseInboundItemDto.class);
                    dto.setId(null);
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setWarehouseName(tuple.get(qWarehouse.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    dto.setCategoryName(tuple.get(qProductCategory.name));
                    dto.setSpec(tuple.get(qProduct.specification));
                    list.add(dto);
                }, List::addAll);
    }

    @Transactional
    public PurchaseOrder save(PurchaseOrderForm purchaseOrderForm, Long merchantId) {
        PurchaseOrder order = purchaseOrderForm.getPurchaseOrder();
        checkoutService.assertEditable(order.getMerchantId(), order.getAccountBookId(), order.getOrderDate());
        if (order.getId() != null) {
            PurchaseOrder original = purchaseOrderRepository.getById(order.getId());
            Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能更新~");
            BeanUtil.copyProperties(order, original, CopyOptions.create().ignoreNullValue());

            Set<Long> ids = new HashSet<>();
            BigDecimal secondarySum = BigDecimal.ZERO;
            for (PurchaseOrderItem d : purchaseOrderForm.getPurchaseOrderItemList()) {
                //计算基本单价（基本单位成本 = 采购单价 / 换算率）
                BigDecimal conversionRate = d.getConversionRate() != null ? d.getConversionRate() : BigDecimal.ONE;
                d.setUnitPrice(d.getSecondaryPrice().divide(conversionRate, 2, RoundingMode.HALF_UP));
                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(order.getAccountBookId());
                d.setPurchaseOrderId(order.getId());
                d.setMerchantId(merchantId);
                //保存更新购货商品价格
                secondarySum = secondarySum.add(d.getSecondaryQuantity());
                savePrice(d, order);
            }
            original.setSecondarySum(secondarySum);
            purchaseOrderItemRepository.saveAll(purchaseOrderForm.getPurchaseOrderItemList());
            return purchaseOrderRepository.save(original);
        } else {
            order.setOrderNo(codeSeedService.generateCode(order.getMerchantId(), order.getAccountBookId(), "采购订单"));

            BigDecimal secondarySum = purchaseOrderForm.getPurchaseOrderItemList()
                    .stream()
                    .map(PurchaseOrderItem::getSecondaryQuantity)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            order.setSecondarySum(secondarySum);

            purchaseOrderRepository.save(order);
            for (PurchaseOrderItem d : purchaseOrderForm.getPurchaseOrderItemList()) {
                //计算基本单价（基本单位成本 = 采购单价 / 换算率）
                BigDecimal conversionRate = d.getConversionRate() != null ? d.getConversionRate() : BigDecimal.ONE;
                d.setUnitPrice(d.getSecondaryPrice().divide(conversionRate, 2, RoundingMode.HALF_UP));
                d.setAccountBookId(order.getAccountBookId());
                d.setPurchaseOrderId(order.getId());
                d.setMerchantId(merchantId);
                //保存更新购货商品价格
                savePrice(d, order);
            }
            purchaseOrderItemRepository.saveAll(purchaseOrderForm.getPurchaseOrderItemList());
            return purchaseOrderRepository.save(purchaseOrderForm.getPurchaseOrder());
        }
    }

    private void savePrice(PurchaseOrderItem item, PurchaseOrder order) {
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(item.getUnitPrice());
        priceRecord.setBaseUnitId(item.getBaseUnitId());
        priceRecord.setProductId(item.getProductId());
        priceRecord.setMerchantId(item.getMerchantId());
        priceRecord.setSupplierId(order.getSupplierId());
        priceRecord.setAccountBookId(order.getAccountBookId());
        priceRecord.setOrderId(order.getId());
        priceRecord.setPriceSource(PriceSource.最近采购价格);
        priceRecord.setPriceType(PriceType.最近采购价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    @Transactional
    public void delete(Long purchaseOrderId, Long merchantId, Long accountBookId) {
        PurchaseOrder original = purchaseOrderRepository.getById(purchaseOrderId);

        Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能删除~");
        Assert.isFalse(original.getPurchaseInboundId() != null, "已关联入库单不能删除~");

        jqf.delete(qPurchaseOrder)
                .where(qPurchaseOrder.id.eq(purchaseOrderId).and(qPurchaseOrder.merchantId.eq(merchantId)).and(qPurchaseOrder.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseOrder> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseOrder).where(qPurchaseOrder.merchantId.eq(merchantId).and(qPurchaseOrder.accountBookId.eq(accountBookId))).fetch();
    }
    //反审核
    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        List<PurchaseOrder> orders = bqf.selectFrom(qPurchaseOrder).where(qPurchaseOrder.merchantId.eq(merchantId).and(qPurchaseOrder.id.in(ids))).fetch();
        Assert.isFalse(CollUtil.isEmpty(orders), "未找到数据~");
        List<Long> setIds = new ArrayList<>();
        if (OrderStatus.已审核.equals(state)) {
            for (PurchaseOrder order : orders) {
                if (OrderStatus.已保存.equals(order.getOrderStatus())) {
                    setIds.add(order.getId());
                } else {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                }
            }
        } else if (OrderStatus.已保存.equals(state)) {
            for (PurchaseOrder order : orders) {
                if (OrderStatus.已审核.equals(order.getOrderStatus()) && order.getPurchaseInboundId() == null) {
                    setIds.add(order.getId());
                } else {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                }
            }
        }
        if (CollUtil.isNotEmpty(setIds)) {
            jqf.update(qPurchaseOrder)
                    .set(qPurchaseOrder.orderStatus, state)
                    .set(qPurchaseOrder.approvedAt, LocalDateTime.now()).
                    set(qPurchaseOrder.approvedBy, adminId)
                    .where(qPurchaseOrder.id.in(setIds))
                    .execute();
        }
    }

    public Dict load(Long merchantId, Long orderId) {
        Tuple fetchFirst = bqf.selectFrom(qPurchaseOrder)
                .select(qPurchaseOrder, qSupplier.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseOrder.supplierId))
                .where(qPurchaseOrder.merchantId.eq(merchantId).and(qPurchaseOrder.id.eq(orderId))).fetchFirst();

        QUnit qUnit1 = new QUnit("id");

        PurchaseOrderDto orderDto = BeanUtil.toBean(fetchFirst.get(qPurchaseOrder), PurchaseOrderDto.class);
        orderDto.setSupplierName(fetchFirst.get(qSupplier.name));
        ArrayList<PurchaseOrderItemDto> collect = jqf.selectFrom(qPurchaseOrderItem)
                .select(qPurchaseOrderItem, qProduct.code, qProduct.name, qWarehouse.name, qProductCategory.name, qProduct.specification,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseOrderItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseOrderItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseOrderItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseOrderItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(qPurchaseOrderItem.purchaseOrderId.eq(orderId).and(qPurchaseOrderItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseOrderItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseOrderItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseOrderItem), PurchaseOrderItemDto.class);
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setCategoryName(tuple.get(qProductCategory.name));
                    dto.setSpec(tuple.get(qProduct.specification));
                    dto.setWarehouseName(tuple.get(qWarehouse.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    list.add(dto);
                }, List::addAll);
        return Dict.create().set("purchaseOrder", orderDto).set("purchaseOrderItemList", collect);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qPurchaseOrder.supplierId.eq(supplierId));
            }
        }

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qPurchaseOrder.orderStatus.eq(state));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qPurchaseOrder.orderNo.contains(filter));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseOrder.orderDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseOrder.orderDate.loe(end));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPurchaseOrder.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPurchaseOrder.accountBookId, accountBookId);
        }
    }

    @Transactional
    public void importData(List<PurchaseOrderImportVo> rows, Long merchantId, Long accountBookId, Long adminId) {
        for (int i = 0; i < rows.size(); i++) {
            PurchaseOrderImportVo row = rows.get(i);
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
        Map<String, List<PurchaseOrderImportVo>> groups = new LinkedHashMap<>();
        for (PurchaseOrderImportVo row : rows) {
            String key = StrUtil.isNotEmpty(row.getOrderNo()) ? row.getOrderNo() : "ROW_" + System.nanoTime();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<PurchaseOrderImportVo>> entry : groups.entrySet()) {
            List<PurchaseOrderImportVo> group = entry.getValue();
            PurchaseOrderImportVo first = group.get(0);

            Supplier supplier;
            if (StrUtil.isNotEmpty(first.getSupplierCode())) {
                supplier = bqf.selectFrom(qSupplier).where(qSupplier.code.eq(first.getSupplierCode()).and(qSupplier.merchantId.eq(merchantId))).fetchFirst();
            } else {
                supplier = bqf.selectFrom(qSupplier).where(qSupplier.name.eq(first.getSupplierName()).and(qSupplier.merchantId.eq(merchantId))).fetchFirst();
            }
            if (supplier == null) throw new ServiceException("供应商「" + (StrUtil.isNotEmpty(first.getSupplierCode()) ? first.getSupplierCode() : first.getSupplierName()) + "」不存在");

            LocalDate orderDate;
            try { orderDate = LocalDate.parse(first.getOrderDate()); } catch (Exception e) { throw new ServiceException("单据日期格式错误：" + first.getOrderDate()); }

            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalDiscountRate = first.getDiscountRate() != null ? first.getDiscountRate() : BigDecimal.ZERO;
            List<PurchaseOrderItem> items = new ArrayList<>();

            for (PurchaseOrderImportVo row : group) {
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

                PurchaseOrderItem item = new PurchaseOrderItem();
                item.setProductId(product.getId()); item.setBaseUnitId(product.getUnitId());
                item.setQuantity(qty); item.setSecondaryQuantity(qty);
                item.setSecondaryUnitId(product.getUnitId()); item.setConversionRate(BigDecimal.ONE);
                item.setUnitPrice(price); item.setDiscountRate(dr); item.setDiscountAmount(da);
                item.setSubtotal(st.subtract(da)); item.setWarehouseId(warehouse != null ? warehouse.getId() : null);
                item.setCreatedBy(adminId); item.setCreatedAt(LocalDateTime.now());
                item.setMerchantId(merchantId); item.setAccountBookId(accountBookId);
                item.setRemark(row.getRemarks()); items.add(item);
                totalSubtotal = totalSubtotal.add(st);
            }

            BigDecimal totalDiscountAmount = totalSubtotal.multiply(totalDiscountRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            PurchaseOrder order = new PurchaseOrder();
            order.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "采购订单"));
            order.setSupplierId(supplier.getId()); order.setOrderDate(orderDate);
            order.setTotalAmount(totalSubtotal); order.setDiscountRate(totalDiscountRate);
            order.setDiscountAmount(totalDiscountAmount); order.setFinalAmount(totalSubtotal.subtract(totalDiscountAmount));
            order.setRemarks(first.getRemarks()); order.setOrderStatus(OrderStatus.已保存);
            order.setCreatedBy(adminId); order.setCreatedAt(LocalDateTime.now());
            order.setMerchantId(merchantId); order.setAccountBookId(accountBookId);
            PurchaseOrder saved = purchaseOrderRepository.save(order);

            for (PurchaseOrderItem item : items) {
                item.setPurchaseOrderId(saved.getId());
                purchaseOrderItemRepository.save(item);
            }
        }
    }
}
