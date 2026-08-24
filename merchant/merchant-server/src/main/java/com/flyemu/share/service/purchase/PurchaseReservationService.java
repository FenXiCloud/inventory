package com.flyemu.share.service.purchase;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.PurchaseReservationDto;
import com.flyemu.share.dto.purchase.PurchaseReservationItemDto;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.purchase.PurchaseReservation;
import com.flyemu.share.entity.purchase.PurchaseReservationItem;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.entity.purchase.PurchaseInboundItem;
import com.flyemu.share.entity.purchase.QPurchaseReservation;
import com.flyemu.share.entity.purchase.QPurchaseReservationItem;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrderItem;
import com.flyemu.share.entity.sales.QSalesReservation;
import com.flyemu.share.entity.sales.QSalesReservationItem;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.PurchaseInboundForm;
import com.flyemu.share.form.PurchaseReservationForm;
import com.flyemu.share.repository.purchase.PurchaseReservationItemRepository;
import com.flyemu.share.repository.purchase.PurchaseReservationRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.setting.CodeSeedService;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseReservationService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QPurchaseReservation qPurchaseReservation = QPurchaseReservation.purchaseReservation;
    private final static QPurchaseReservationItem qPurchaseReservationItem = QPurchaseReservationItem.purchaseReservationItem;
    private final static QSalesReservation qSalesReservation = QSalesReservation.salesReservation;
    private final static QSalesReservationItem qSalesReservationItem = QSalesReservationItem.salesReservationItem;
    private final static QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
    private final static QPurchaseOrderItem qPurchaseOrderItem = QPurchaseOrderItem.purchaseOrderItem;

    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final PurchaseReservationRepository purchaseReservationRepository;
    private final PurchaseReservationItemRepository purchaseReservationItemRepository;
    private final PurchaseInboundService purchaseInboundService;
    private final CodeSeedService codeSeedService;

    public PageResults<PurchaseReservationDto> query(Page page, Query query) {
        long totalSize = bqf.selectFrom(qPurchaseReservation)
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qPurchaseReservation)
                .select(qPurchaseReservation, qSupplier.name, qMerchantUser.name)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseReservation.createdBy))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReservation.supplierId))
                .where(query.builder)
                .orderBy(qPurchaseReservation.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<PurchaseReservationDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReservationDto dto = BeanUtil.toBean(tuple.get(qPurchaseReservation), PurchaseReservationDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));

            List<PurchaseReservationItem> itemList = bqf.selectFrom(qPurchaseReservationItem)
                    .select(qPurchaseReservationItem)
                    .where(qPurchaseReservationItem.purchaseReservationId.eq(dto.getId()))
                    .fetch();
            List<PurchaseReservationItemDto> itemDTOs = new ArrayList<>();
            BigDecimal totalQuantity = BigDecimal.ZERO;
            for (PurchaseReservationItem item : itemList) {
                PurchaseReservationItemDto itemDTO = BeanUtil.toBean(item, PurchaseReservationItemDto.class);
                itemDTOs.add(itemDTO);
                if (itemDTO.getQuantity() != null) {
                    totalQuantity = totalQuantity.add(itemDTO.getQuantity());
                }
            }
            dto.setPurchaseReservationItemList(itemDTOs);
            dto.setTotalQuantity(totalQuantity);
            applyStatusText(dto);
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public PurchaseReservation save(PurchaseReservationForm form, Long merchantId) {
        PurchaseReservation purchaseReservation = form.getPurchaseReservation();
        checkoutService.assertEditable(purchaseReservation.getMerchantId(), purchaseReservation.getAccountBookId(), purchaseReservation.getOrderDate());
        purchaseReservation.setMerchantId(merchantId);
        Long id = purchaseReservation.getId();
        List<PurchaseReservationItem> itemList = form.getPurchaseReservationItemList();

        if (id != null) {
            PurchaseReservation original = bqf.selectFrom(qPurchaseReservation)
                    .where(qPurchaseReservation.id.eq(id).and(qPurchaseReservation.merchantId.eq(merchantId)))
                    .fetchFirst();
            if (original == null) {
                throw new ServiceException("单据不存在");
            }
            OrderStatus orderStatus = original.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已审核)) {
                throw new InvalidContextException("已审核单据不能修改");
            }
            BeanUtil.copyProperties(purchaseReservation, original, CopyOptions.create().ignoreNullValue());
            PurchaseReservation update = purchaseReservationRepository.save(original);
            jqf.delete(qPurchaseReservationItem).where(qPurchaseReservationItem.purchaseReservationId.eq(id)).execute();
            if (!CollectionUtils.isEmpty(itemList)) {
                itemList.forEach(item -> {
                    item.setPurchaseReservationId(update.getId());
                    item.setAccountBookId(purchaseReservation.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setUpdatedAt(LocalDateTime.now());
                });
                purchaseReservationItemRepository.saveAll(itemList);
            }
            return update;
        } else {
            purchaseReservation.setOrderStatus(OrderStatus.已保存);
            purchaseReservation.setStatus(0);
            purchaseReservation.setOrderNo(codeSeedService.generateCode(merchantId, purchaseReservation.getAccountBookId(), "进货预订"));
            PurchaseReservation save = purchaseReservationRepository.save(purchaseReservation);
            if (!CollectionUtils.isEmpty(itemList)) {
                itemList.forEach(item -> {
                    item.setPurchaseReservationId(save.getId());
                    item.setAccountBookId(purchaseReservation.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setCreatedBy(purchaseReservation.getCreatedBy());
                    item.setCreatedAt(purchaseReservation.getCreatedAt());
                    item.setQuantityOrdered(BigDecimal.ZERO);
                });
                purchaseReservationItemRepository.saveAll(itemList);
            }
            return save;
        }
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        PurchaseReservation original = purchaseReservationRepository.getById(id);
        OrderStatus orderStatus = original.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已审核)) {
            throw new InvalidContextException("已审核单据不能删除");
        }
        jqf.delete(qPurchaseReservation)
                .where(qPurchaseReservation.id.eq(id).and(qPurchaseReservation.merchantId.eq(merchantId)).and(qPurchaseReservation.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qPurchaseReservationItem)
                .where(qPurchaseReservationItem.purchaseReservationId.eq(id).and(qPurchaseReservationItem.merchantId.eq(merchantId)).and(qPurchaseReservationItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public PurchaseReservationDto load(Long merchantId, Long orderId) {
        PurchaseReservation purchaseReservation = bqf.selectFrom(qPurchaseReservation)
                .where(qPurchaseReservation.merchantId.eq(merchantId).and(qPurchaseReservation.id.eq(orderId)))
                .fetchFirst();
        if (purchaseReservation == null) {
            throw new ServiceException("单据不存在");
        }
        PurchaseReservationDto dto = BeanUtil.toBean(purchaseReservation, PurchaseReservationDto.class);
        List<Tuple> fetch = jqf.selectFrom(qPurchaseReservationItem)
                .select(qPurchaseReservationItem, qProduct.code, qProduct.name, qProduct.specification, qUnit.name, qWarehouse.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReservationItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReservationItem.baseUnitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseReservationItem.warehouseId))
                .where(qPurchaseReservationItem.purchaseReservationId.eq(orderId)
                        .and(qPurchaseReservationItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseReservationItem.id.asc()).fetch();
        List<PurchaseReservationItemDto> itemDTOs = new ArrayList<>();
        fetch.forEach(tuple -> {
            PurchaseReservationItemDto itemDTO = BeanUtil.toBean(tuple.get(qPurchaseReservationItem), PurchaseReservationItemDto.class);
            itemDTO.setProductName(tuple.get(qProduct.name));
            itemDTO.setProductCode(tuple.get(qProduct.code));
            itemDTO.setSpecification(tuple.get(qProduct.specification));
            itemDTO.setUnitName(tuple.get(qUnit.name));
            itemDTO.setWarehouseName(tuple.get(qWarehouse.name));
            itemDTOs.add(itemDTO);
        });
        dto.setPurchaseReservationItemList(itemDTOs);

        // 来源销售预订号（由明细反查）
        Long sourceSalesReservationId = itemDTOs.stream()
                .map(PurchaseReservationItemDto::getSalesReservationId)
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (sourceSalesReservationId != null) {
            String sourceNo = bqf.selectFrom(qSalesReservation)
                    .select(qSalesReservation.orderNo)
                    .where(qSalesReservation.id.eq(sourceSalesReservationId))
                    .fetchFirst();
            dto.setSourceSalesReservationNo(sourceNo);
        }

        applyStatusText(dto);
        return dto;
    }

    /**
     * 根据订单状态与转采购状态推导展示文本
     */
    private void applyStatusText(PurchaseReservationDto dto) {
        OrderStatus os = dto.getOrderStatus();
        Integer st = dto.getStatus();
        if (OrderStatus.已审核.equals(os)) {
            dto.setOrderStatusText(st != null && st >= 2 ? "已转采购" : "已审核");
        } else {
            dto.setOrderStatusText("待审核");
        }
        if (st == null || st == 0) {
            dto.setStatusText("未转");
        } else if (st == 1) {
            dto.setStatusText("部分转");
        } else {
            dto.setStatusText("已转");
        }
    }

    /**
     * 转进货单：根据进货预订生成采购入库单并自动审核（入库）
     */
    @Transactional
    public Long transferToPurchaseInbound(Long purchaseReservationId, Long warehouseId, Long merchantId, Long adminId) {
        PurchaseReservation reservation = bqf.selectFrom(qPurchaseReservation)
                .where(qPurchaseReservation.id.eq(purchaseReservationId).and(qPurchaseReservation.merchantId.eq(merchantId)))
                .fetchFirst();
        if (reservation == null) {
            throw new ServiceException("进货预订不存在");
        }
        if (!OrderStatus.已审核.equals(reservation.getOrderStatus())) {
            throw new ServiceException("进货预订未审核，不能转进货单");
        }
        if (reservation.getStatus() != null && reservation.getStatus() >= 2) {
            throw new ServiceException("该进货预订已全部转采购入库，不能重复转换");
        }

        List<PurchaseReservationItem> items = bqf.selectFrom(qPurchaseReservationItem)
                .where(qPurchaseReservationItem.purchaseReservationId.eq(purchaseReservationId)
                        .and(qPurchaseReservationItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseReservationItem.id.asc())
                .fetch();
        if (CollectionUtils.isEmpty(items)) {
            throw new ServiceException("进货预订无明细，无法转进货单");
        }

        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setMerchantId(merchantId);
        inbound.setAccountBookId(reservation.getAccountBookId());
        inbound.setSupplierId(reservation.getSupplierId());
        inbound.setInboundDate(LocalDate.now());
        inbound.setTotalAmount(reservation.getTotalAmount());
        inbound.setDiscountRate(reservation.getDiscountRate());
        inbound.setDiscountAmount(reservation.getDiscountAmount());
        inbound.setFinalAmount(reservation.getFinalAmount());
        inbound.setRemarks(reservation.getRemarks());
        inbound.setCreatedBy(adminId);
        inbound.setCreatedAt(LocalDateTime.now());

        List<PurchaseInboundItem> inboundItems = new ArrayList<>();
        for (PurchaseReservationItem item : items) {
            PurchaseInboundItem ii = new PurchaseInboundItem();
            ii.setProductId(item.getProductId());
            ii.setBaseUnitId(item.getBaseUnitId());
            ii.setQuantity(nvl(item.getQuantity()));
            ii.setSecondaryUnitId(item.getSecondaryUnitId());
            ii.setSecondaryQuantity(nvl(item.getSecondaryQuantity()));
            ii.setConversionRate(item.getConversionRate());
            ii.setSecondaryPrice(auxPriceOf(item));
            ii.setUnitPrice(item.getUnitPrice());
            ii.setDiscountRate(item.getDiscountRate());
            ii.setDiscountAmount(item.getDiscountValue());
            ii.setSubtotal(item.getSubtotal());
            ii.setWarehouseId(warehouseId != null ? warehouseId : item.getWarehouseId());
            ii.setMerchantId(merchantId);
            ii.setAccountBookId(item.getAccountBookId());
            ii.setCreatedBy(adminId);
            ii.setCreatedAt(LocalDateTime.now());
            inboundItems.add(ii);
        }

        PurchaseInboundForm form = new PurchaseInboundForm();
        form.setPurchaseInbound(inbound);
        form.setPurchaseInboundItemList(inboundItems);
        PurchaseInbound saved = purchaseInboundService.save(form, merchantId);
        purchaseInboundService.approved(List.of(saved.getId()), OrderStatus.已审核, adminId, merchantId);

        // 回写进货预订：全部转采购
        reservation.setStatus(2);
        reservation.setPurchaseOrderId(saved.getId());
        purchaseReservationRepository.save(reservation);
        jqf.update(qPurchaseReservationItem)
                .set(qPurchaseReservationItem.quantityOrdered, qPurchaseReservationItem.quantity)
                .where(qPurchaseReservationItem.purchaseReservationId.eq(purchaseReservationId)
                        .and(qPurchaseReservationItem.merchantId.eq(merchantId)))
                .execute();

        return saved.getId();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 取进货预订明细的辅助单位单价，为空或 0 时由金额反推
     */
    private BigDecimal auxPriceOf(PurchaseReservationItem item) {
        BigDecimal price = nvl(item.getUnitPrice());
        if (price.compareTo(BigDecimal.ZERO) == 0) {
            BigDecimal amount = nvl(item.getSubtotal()).add(nvl(item.getDiscountValue()));
            BigDecimal qty = nvl(item.getSecondaryQuantity());
            if (amount.compareTo(BigDecimal.ZERO) > 0 && qty.compareTo(BigDecimal.ZERO) > 0) {
                price = amount.divide(qty, 2, RoundingMode.HALF_UP);
            }
        }
        return price;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择单据");
        }
        List<PurchaseReservation> list = bqf.selectFrom(qPurchaseReservation)
                .where(qPurchaseReservation.merchantId.eq(merchantId).and(qPurchaseReservation.id.in(ids)))
                .fetch();
        if (list.isEmpty()) {
            throw new ServiceException("未找到数据~");
        }
        list.forEach(order -> {
            if (OrderStatus.已保存.equals(state)) {
                // 反审核时检查是否已转采购订单
                if (order.getStatus() != null && order.getStatus() > 0) {
                    throw new InvalidContextException("已转采购订单不能反审核");
                }
            }
            order.setOrderStatus(state);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(adminId);
        });
        purchaseReservationRepository.saveAll(list);
    }

    /**
     * 可转采购订单的列表：已审核且未全部转采购
     */
    public PageResults<PurchaseReservationDto> queryToPurchaseOrder(Page page, Query query) {
        BooleanBuilder builder = query.builder.and(qPurchaseReservation.status.ne(2));
        long totalSize = bqf.selectFrom(qPurchaseReservation)
                .where(builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qPurchaseReservation)
                .select(qPurchaseReservation, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReservation.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseReservation.createdBy))
                .where(builder)
                .orderBy(qPurchaseReservation.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<PurchaseReservationDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReservationDto dto = BeanUtil.toBean(tuple.get(qPurchaseReservation), PurchaseReservationDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });
        return new PageResults<>(dtos, page, totalSize);
    }

    /**
     * 选中进货预订后，生成可采购明细（数量为剩余可转数量）
     */
    public List<Map<String, Object>> loadToPurchaseOrder(List<Long> reservationIds, Long merchantId) {
        if (CollectionUtils.isEmpty(reservationIds)) {
            return new ArrayList<>();
        }
        List<Tuple> rows = bqf.selectFrom(qPurchaseReservationItem)
                .select(qPurchaseReservationItem, qPurchaseReservation.orderNo, qPurchaseReservation.supplierId, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qPurchaseReservation).on(qPurchaseReservation.id.eq(qPurchaseReservationItem.purchaseReservationId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReservationItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReservationItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .where(qPurchaseReservationItem.purchaseReservationId.in(reservationIds)
                        .and(qPurchaseReservationItem.merchantId.eq(merchantId))
                        .and(qPurchaseReservation.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseReservation.status.ne(2)))
                .orderBy(qPurchaseReservationItem.id.asc())
                .fetch();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple tuple : rows) {
            PurchaseReservationItem item = tuple.get(qPurchaseReservationItem);
            BigDecimal orderQty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal orderedQty = item.getQuantityOrdered() == null ? BigDecimal.ZERO : item.getQuantityOrdered();
            BigDecimal remain = orderQty.subtract(orderedQty);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            Map<String, Object> map = new HashMap<>();
            map.put("productId", item.getProductId());
            map.put("baseUnitId", item.getBaseUnitId());
            map.put("quantity", remain);
            map.put("secondaryUnitId", item.getSecondaryUnitId());
            map.put("secondaryQuantity", remain);
            map.put("conversionRate", item.getConversionRate());
            map.put("unitPrice", item.getUnitPrice());
            map.put("discountRate", item.getDiscountRate());
            map.put("discountValue", item.getDiscountValue());
            map.put("subtotal", item.getSubtotal());
            map.put("warehouseId", item.getWarehouseId());
            map.put("purchaseReservationId", item.getPurchaseReservationId());
            map.put("purchaseReservationItemId", item.getId());
            map.put("merchantId", merchantId);
            map.put("accountBookId", item.getAccountBookId());
            result.add(map);
        }
        return result;
    }

    /**
     * 选中进货预订后，生成可采购入库明细（数量为剩余可转数量）
     */
    public List<PurchaseInboundItem> loadToPurchaseInbound(List<Long> reservationIds, Long merchantId) {
        if (CollectionUtils.isEmpty(reservationIds)) {
            return new ArrayList<>();
        }
        List<Tuple> rows = bqf.selectFrom(qPurchaseReservationItem)
                .select(qPurchaseReservationItem, qPurchaseReservation.orderNo, qPurchaseReservation.supplierId, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qPurchaseReservation).on(qPurchaseReservation.id.eq(qPurchaseReservationItem.purchaseReservationId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReservationItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReservationItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .where(qPurchaseReservationItem.purchaseReservationId.in(reservationIds)
                        .and(qPurchaseReservationItem.merchantId.eq(merchantId))
                        .and(qPurchaseReservation.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseReservation.status.ne(2)))
                .orderBy(qPurchaseReservationItem.id.asc())
                .fetch();

        List<PurchaseInboundItem> result = new ArrayList<>();
        for (Tuple tuple : rows) {
            PurchaseReservationItem item = tuple.get(qPurchaseReservationItem);
            BigDecimal orderQty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal orderedQty = item.getQuantityOrdered() == null ? BigDecimal.ZERO : item.getQuantityOrdered();
            BigDecimal remain = orderQty.subtract(orderedQty);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PurchaseInboundItem inboundItem = new PurchaseInboundItem();
            inboundItem.setProductId(item.getProductId());
            inboundItem.setBaseUnitId(item.getBaseUnitId());
            inboundItem.setQuantity(remain);
            inboundItem.setSecondaryUnitId(item.getSecondaryUnitId());
            inboundItem.setSecondaryQuantity(remain);
            inboundItem.setConversionRate(item.getConversionRate());
            inboundItem.setUnitPrice(item.getUnitPrice());
            inboundItem.setSecondaryPrice(item.getUnitPrice());
            inboundItem.setDiscountRate(item.getDiscountRate());
            inboundItem.setDiscountAmount(item.getDiscountValue());
            inboundItem.setSubtotal(item.getSubtotal());
            inboundItem.setWarehouseId(item.getWarehouseId());
            inboundItem.setMerchantId(merchantId);
            inboundItem.setAccountBookId(item.getAccountBookId());
            result.add(inboundItem);
        }
        return result;
    }

    public Map<String, BigDecimal> queryTotal(Query query) {
        BigDecimal amount = bqf.selectFrom(qPurchaseReservation)
                .select(qPurchaseReservation.finalAmount.sum())
                .where(query.builder).fetchFirst();
        BigDecimal quantity = bqf.selectFrom(qPurchaseReservationItem)
                .select(qPurchaseReservationItem.quantity.sum())
                .where(qPurchaseReservationItem.purchaseReservationId.in(
                        bqf.selectFrom(qPurchaseReservation).select(qPurchaseReservation.id).where(query.builder)
                )).fetchFirst();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", amount);
        result.put("quantity", java.util.Objects.requireNonNullElse(quantity, BigDecimal.ZERO));
        return result;
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPurchaseReservation.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPurchaseReservation.accountBookId, accountBookId);
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qPurchaseReservation.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qPurchaseReservation.orderStatus.eq(state));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseReservation.orderDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseReservation.orderDate.loe(end));
            }
        }

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qPurchaseReservation.supplierId.eq(supplierId));
            }
        }
    }
}
