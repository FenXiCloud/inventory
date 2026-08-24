package com.flyemu.share.service.sales;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.sales.SalesReservationDto;
import com.flyemu.share.dto.sales.SalesReservationItemDto;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.purchase.PurchaseReservation;
import com.flyemu.share.entity.purchase.PurchaseReservationItem;
import com.flyemu.share.entity.purchase.QPurchaseReservation;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.sales.SalesOutboundItem;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.repository.sales.SalesOrderRepository;
import com.flyemu.share.repository.sales.SalesOrderItemRepository;
import com.flyemu.share.entity.purchase.QPurchaseReservationItem;
import com.flyemu.share.entity.sales.QSalesReservation;
import com.flyemu.share.entity.sales.QSalesReservationItem;
import com.flyemu.share.entity.sales.SalesReservation;
import com.flyemu.share.entity.sales.SalesReservationItem;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.form.SalesReservationForm;
import com.flyemu.share.repository.purchase.PurchaseReservationItemRepository;
import com.flyemu.share.repository.purchase.PurchaseReservationRepository;
import com.flyemu.share.repository.sales.SalesReservationItemRepository;
import com.flyemu.share.repository.sales.SalesReservationRepository;
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
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReservationService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QSalesReservation qSalesReservation = QSalesReservation.salesReservation;
    private final static QSalesReservationItem qSalesReservationItem = QSalesReservationItem.salesReservationItem;
    private final static QPurchaseReservationItem qPurchaseReservationItem = QPurchaseReservationItem.purchaseReservationItem;
    private final static QPurchaseReservation qPurchaseReservation = QPurchaseReservation.purchaseReservation;

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final SalesReservationRepository salesReservationRepository;
    private final SalesReservationItemRepository salesReservationItemRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final PurchaseReservationRepository purchaseReservationRepository;
    private final PurchaseReservationItemRepository purchaseReservationItemRepository;
    private final SalesOutboundService salesOutboundService;
    private final CodeSeedService codeSeedService;

    public PageResults<SalesReservationDto> query(Page page, Query query) {
        long totalSize = bqf.selectFrom(qSalesReservation)
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesReservation)
                .select(qSalesReservation, qCustomer.name, qMerchantUser.name)
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesReservation.createdBy))
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesReservation.customerId))
                .where(query.builder)
                .orderBy(qSalesReservation.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesReservationDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesReservationDto dto = BeanUtil.toBean(tuple.get(qSalesReservation), SalesReservationDto.class);
            dto.setCustomerName(tuple.get(qCustomer.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));

            List<SalesReservationItem> itemList = bqf.selectFrom(qSalesReservationItem)
                    .select(qSalesReservationItem)
                    .where(qSalesReservationItem.salesReservationId.eq(dto.getId()))
                    .fetch();
            List<SalesReservationItemDto> itemDTOs = new ArrayList<>();
            BigDecimal totalQuantity = BigDecimal.ZERO;
            for (SalesReservationItem item : itemList) {
                SalesReservationItemDto itemDTO = BeanUtil.toBean(item, SalesReservationItemDto.class);
                itemDTOs.add(itemDTO);
                if (itemDTO.getQuantity() != null) {
                    totalQuantity = totalQuantity.add(itemDTO.getQuantity());
                }
            }
            dto.setSalesReservationItemList(itemDTOs);
            dto.setTotalQuantity(totalQuantity);
            applyStatusText(dto);
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public SalesReservation save(SalesReservationForm form, Long merchantId) {
        SalesReservation salesReservation = form.getSalesReservation();
        checkoutService.assertEditable(salesReservation.getMerchantId(), salesReservation.getAccountBookId(), salesReservation.getOrderDate());
        salesReservation.setMerchantId(merchantId);
        Long id = salesReservation.getId();
        List<SalesReservationItem> itemList = form.getSalesReservationItemList();

        if (id != null) {
            SalesReservation original = bqf.selectFrom(qSalesReservation)
                    .where(qSalesReservation.id.eq(id).and(qSalesReservation.merchantId.eq(merchantId)))
                    .fetchFirst();
            if (original == null) {
                throw new ServiceException("单据不存在");
            }
            OrderStatus orderStatus = original.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已审核)) {
                throw new InvalidContextException("已审核单据不能修改");
            }
            BeanUtil.copyProperties(salesReservation, original, CopyOptions.create().ignoreNullValue());
            SalesReservation update = salesReservationRepository.save(original);
            jqf.delete(qSalesReservationItem).where(qSalesReservationItem.salesReservationId.eq(id)).execute();
            if (!CollectionUtils.isEmpty(itemList)) {
                itemList.forEach(item -> {
                    item.setSalesReservationId(update.getId());
                    item.setAccountBookId(salesReservation.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setUpdatedAt(LocalDateTime.now());
                });
                salesReservationItemRepository.saveAll(itemList);
            }
            return update;
        } else {
            salesReservation.setOrderStatus(OrderStatus.已保存);
            salesReservation.setStatus(0);
            salesReservation.setOrderNo(codeSeedService.generateCode(merchantId, salesReservation.getAccountBookId(), "销售预订"));
            SalesReservation save = salesReservationRepository.save(salesReservation);
            if (!CollectionUtils.isEmpty(itemList)) {
                itemList.forEach(item -> {
                    item.setSalesReservationId(save.getId());
                    item.setAccountBookId(salesReservation.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setCreatedBy(salesReservation.getCreatedBy());
                    item.setCreatedAt(salesReservation.getCreatedAt());
                    item.setQuantityPurchased(BigDecimal.ZERO);
                });
                salesReservationItemRepository.saveAll(itemList);
            }
            return save;
        }
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        SalesReservation original = salesReservationRepository.getById(id);
        OrderStatus orderStatus = original.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已审核)) {
            throw new InvalidContextException("已审核单据不能删除");
        }
        jqf.delete(qSalesReservation)
                .where(qSalesReservation.id.eq(id).and(qSalesReservation.merchantId.eq(merchantId)).and(qSalesReservation.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qSalesReservationItem)
                .where(qSalesReservationItem.salesReservationId.eq(id).and(qSalesReservationItem.merchantId.eq(merchantId)).and(qSalesReservationItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public SalesReservationDto load(Long merchantId, Long orderId) {
        SalesReservation salesReservation = bqf.selectFrom(qSalesReservation)
                .where(qSalesReservation.merchantId.eq(merchantId).and(qSalesReservation.id.eq(orderId)))
                .fetchFirst();
        if (salesReservation == null) {
            throw new ServiceException("单据不存在");
        }
        SalesReservationDto dto = BeanUtil.toBean(salesReservation, SalesReservationDto.class);
        List<Tuple> fetch = jqf.selectFrom(qSalesReservationItem)
                .select(qSalesReservationItem, qProduct.code, qProduct.name, qProduct.specification, qUnit.name, qWarehouse.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesReservationItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesReservationItem.baseUnitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qSalesReservationItem.warehouseId))
                .where(qSalesReservationItem.salesReservationId.eq(orderId)
                        .and(qSalesReservationItem.merchantId.eq(merchantId)))
                .orderBy(qSalesReservationItem.id.asc()).fetch();
        List<SalesReservationItemDto> itemDTOs = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesReservationItemDto itemDTO = BeanUtil.toBean(tuple.get(qSalesReservationItem), SalesReservationItemDto.class);
            itemDTO.setProductName(tuple.get(qProduct.name));
            itemDTO.setProductCode(tuple.get(qProduct.code));
            itemDTO.setSpecification(tuple.get(qProduct.specification));
            itemDTO.setUnitName(tuple.get(qUnit.name));
            itemDTO.setWarehouseName(tuple.get(qWarehouse.name));
            itemDTOs.add(itemDTO);
        });
        dto.setSalesReservationItemList(itemDTOs);
        applyStatusText(dto);
        return dto;
    }

    /**
     * 根据订单状态与转进货状态推导展示文本
     */
    private void applyStatusText(SalesReservationDto dto) {
        OrderStatus os = dto.getOrderStatus();
        Integer st = dto.getStatus();
        if (dto.getSalesOrderId() != null) {
            dto.setOrderStatusText("已完成");
        } else if (OrderStatus.已审核.equals(os)) {
            dto.setOrderStatusText(st != null && st >= 2 ? "已转进货" : "已审核");
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
     * 转进货预订：根据销售预订生成一张进货预订（待审核），并建立来源关联
     */
    @Transactional
    public Long transferToPurchase(Long salesReservationId, Long supplierId, Long merchantId, Long adminId) {
        SalesReservation reservation = bqf.selectFrom(qSalesReservation)
                .where(qSalesReservation.id.eq(salesReservationId).and(qSalesReservation.merchantId.eq(merchantId)))
                .fetchFirst();
        if (reservation == null) {
            throw new ServiceException("销售预订不存在");
        }
        if (!OrderStatus.已审核.equals(reservation.getOrderStatus())) {
            throw new ServiceException("销售预订未审核，不能转进货预订");
        }
        if (reservation.getStatus() != null && reservation.getStatus() >= 2) {
            throw new ServiceException("该销售预订已全部转进货预订，不能重复转换");
        }
        if (supplierId == null) {
            throw new ServiceException("请选择供货商");
        }

        List<SalesReservationItem> items = bqf.selectFrom(qSalesReservationItem)
                .where(qSalesReservationItem.salesReservationId.eq(salesReservationId)
                        .and(qSalesReservationItem.merchantId.eq(merchantId)))
                .orderBy(qSalesReservationItem.id.asc())
                .fetch();
        if (CollectionUtils.isEmpty(items)) {
            throw new ServiceException("销售预订无明细，无法转进货预订");
        }

        checkoutService.assertEditable(merchantId, reservation.getAccountBookId(), LocalDate.now());

        PurchaseReservation purchaseReservation = new PurchaseReservation();
        purchaseReservation.setMerchantId(merchantId);
        purchaseReservation.setAccountBookId(reservation.getAccountBookId());
        purchaseReservation.setOrderNo(codeSeedService.generateCode(merchantId, reservation.getAccountBookId(), "进货预订"));
        purchaseReservation.setSupplierId(supplierId);
        purchaseReservation.setOrderDate(LocalDate.now());
        purchaseReservation.setTotalAmount(reservation.getTotalAmount());
        purchaseReservation.setDiscountRate(reservation.getDiscountRate());
        purchaseReservation.setDiscountAmount(reservation.getDiscountAmount());
        purchaseReservation.setFinalAmount(reservation.getFinalAmount());
        purchaseReservation.setRemarks(reservation.getRemarks());
        purchaseReservation.setOrderStatus(OrderStatus.已保存);
        purchaseReservation.setStatus(0);
        purchaseReservation.setCreatedBy(adminId);
        purchaseReservation.setCreatedAt(LocalDateTime.now());
        PurchaseReservation saved = purchaseReservationRepository.save(purchaseReservation);

        List<PurchaseReservationItem> purchaseItems = new ArrayList<>();
        for (SalesReservationItem item : items) {
            PurchaseReservationItem pItem = new PurchaseReservationItem();
            pItem.setPurchaseReservationId(saved.getId());
            pItem.setProductId(item.getProductId());
            pItem.setBaseUnitId(item.getBaseUnitId());
            pItem.setQuantity(nvl(item.getQuantity()));
            pItem.setQuantityOrdered(BigDecimal.ZERO);
            pItem.setSecondaryUnitId(item.getSecondaryUnitId());
            pItem.setSecondaryQuantity(nvl(item.getSecondaryQuantity()));
            pItem.setConversionRate(item.getConversionRate());
            pItem.setUnitPrice(item.getUnitPrice());
            pItem.setDiscountRate(item.getDiscountRate());
            pItem.setDiscountValue(item.getDiscountValue());
            pItem.setSubtotal(item.getSubtotal());
            pItem.setWarehouseId(item.getWarehouseId());
            pItem.setSalesReservationId(item.getSalesReservationId());
            pItem.setSalesReservationItemId(item.getId());
            pItem.setRemark(item.getRemark());
            pItem.setMerchantId(merchantId);
            pItem.setAccountBookId(item.getAccountBookId());
            pItem.setCreatedBy(adminId);
            pItem.setCreatedAt(LocalDateTime.now());
            purchaseItems.add(pItem);
        }
        purchaseReservationItemRepository.saveAll(purchaseItems);

        // 回写销售预订：全部转进货
        reservation.setStatus(2);
        salesReservationRepository.save(reservation);
        jqf.update(qSalesReservationItem)
                .set(qSalesReservationItem.quantityPurchased, qSalesReservationItem.quantity)
                .where(qSalesReservationItem.salesReservationId.eq(salesReservationId)
                        .and(qSalesReservationItem.merchantId.eq(merchantId)))
                .execute();

        return saved.getId();
    }

    /**
     * 转销售单：生成销售出库单并自动审核（扣减库存）
     */
    @Transactional
    public Long transferToSalesOrder(Long salesReservationId, Long merchantId, Long adminId) {
        SalesReservation reservation = bqf.selectFrom(qSalesReservation)
                .where(qSalesReservation.id.eq(salesReservationId).and(qSalesReservation.merchantId.eq(merchantId)))
                .fetchFirst();
        if (reservation == null) {
            throw new ServiceException("销售预订不存在");
        }
        if (!OrderStatus.已审核.equals(reservation.getOrderStatus())) {
            throw new ServiceException("销售预订未审核，不能转销售单");
        }
        if (reservation.getSalesOrderId() != null) {
            throw new ServiceException("该销售预订已转销售单，不能重复转换");
        }

        // 严格校验进货完成：关联的进货预订必须已全部转采购入库
        List<Long> purchaseReservationIds = jqf.select(qPurchaseReservationItem.purchaseReservationId)
                .from(qPurchaseReservationItem)
                .where(qPurchaseReservationItem.salesReservationId.eq(salesReservationId)
                        .and(qPurchaseReservationItem.merchantId.eq(merchantId)))
                .fetch();
        if (CollectionUtils.isEmpty(purchaseReservationIds)) {
            throw new ServiceException("尚未生成进货预订，请先转进货预订并完成采购入库");
        }
        List<PurchaseReservation> purchaseReservations = bqf.selectFrom(qPurchaseReservation)
                .where(qPurchaseReservation.id.in(purchaseReservationIds))
                .fetch();
        for (PurchaseReservation pr : purchaseReservations) {
            if (pr.getStatus() == null || pr.getStatus() < 2) {
                throw new ServiceException("关联进货预订尚未全部转采购入库，不能转销售单");
            }
        }

        List<SalesReservationItem> items = bqf.selectFrom(qSalesReservationItem)
                .where(qSalesReservationItem.salesReservationId.eq(salesReservationId)
                        .and(qSalesReservationItem.merchantId.eq(merchantId)))
                .orderBy(qSalesReservationItem.id.asc())
                .fetch();
        if (CollectionUtils.isEmpty(items)) {
            throw new ServiceException("销售预订无明细，无法转销售单");
        }

        SalesOutbound outbound = new SalesOutbound();
        outbound.setMerchantId(merchantId);
        outbound.setAccountBookId(reservation.getAccountBookId());
        outbound.setCustomerId(reservation.getCustomerId());
        outbound.setOutboundDate(LocalDate.now());
        outbound.setTotalAmount(reservation.getTotalAmount());
        outbound.setDiscountAmount(reservation.getDiscountAmount());
        outbound.setFinalAmount(reservation.getFinalAmount());
        outbound.setDiscountRate(reservation.getDiscountRate());
        outbound.setRemarks(reservation.getRemarks());
        outbound.setCreatedBy(adminId);
        outbound.setCreatedAt(LocalDateTime.now());

        List<SalesOutboundItem> outboundItems = new ArrayList<>();
        for (SalesReservationItem item : items) {
            SalesOutboundItem oi = new SalesOutboundItem();
            oi.setProductId(item.getProductId());
            oi.setBaseUnitId(item.getBaseUnitId());
            oi.setQuantity(nvl(item.getQuantity()));
            oi.setSecondaryUnitId(item.getSecondaryUnitId());
            oi.setSecondaryQuantity(nvl(item.getSecondaryQuantity()));
            oi.setConversionRate(item.getConversionRate());
            oi.setUnitPrice(auxPriceOf(item));
            oi.setDiscountRate(item.getDiscountRate());
            oi.setDiscountValue(item.getDiscountValue());
            oi.setSubtotal(item.getSubtotal());
            oi.setWarehouseId(item.getWarehouseId());
            oi.setRemark(item.getRemark());
            oi.setMerchantId(merchantId);
            oi.setAccountBookId(item.getAccountBookId());
            oi.setCreatedBy(adminId);
            oi.setCreatedAt(LocalDateTime.now());
            outboundItems.add(oi);
        }

        SalesOutboundForm form = new SalesOutboundForm();
        form.setSalesOutbound(outbound);
        form.setSalesOutboundItemList(outboundItems);
        SalesOutbound saved = salesOutboundService.save(form, merchantId);
        salesOutboundService.approved(List.of(saved.getId()), OrderStatus.已审核, adminId, merchantId);

        reservation.setSalesOrderId(saved.getId());
        salesReservationRepository.save(reservation);

        return saved.getId();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 取销售预订明细的辅助单位单价（用于销售出库单的销售单价），为空或 0 时由金额反推
     */
    private BigDecimal auxPriceOf(SalesReservationItem item) {
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
        List<SalesReservation> list = bqf.selectFrom(qSalesReservation)
                .where(qSalesReservation.merchantId.eq(merchantId).and(qSalesReservation.id.in(ids)))
                .fetch();
        if (list.isEmpty()) {
            throw new ServiceException("未找到数据~");
        }
        list.forEach(order -> {
            if (OrderStatus.已保存.equals(state)) {
                // 反审核时检查是否已转进货预订
                if (order.getStatus() != null && order.getStatus() > 0) {
                    throw new InvalidContextException("已转进货预订不能反审核");
                }
            }
            order.setOrderStatus(state);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(adminId);
        });
        salesReservationRepository.saveAll(list);
    }

    /**
     * 可转进货预订的列表：已审核且未全部转进货
     */
    public PageResults<SalesReservationDto> queryToPurchase(Page page, Query query) {
        BooleanBuilder builder = query.builder.and(qSalesReservation.status.ne(2));
        long totalSize = bqf.selectFrom(qSalesReservation)
                .where(builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesReservation)
                .select(qSalesReservation, qCustomer.name, qMerchantUser.name)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesReservation.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesReservation.createdBy))
                .where(builder)
                .orderBy(qSalesReservation.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesReservationDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesReservationDto dto = BeanUtil.toBean(tuple.get(qSalesReservation), SalesReservationDto.class);
            dto.setCustomerName(tuple.get(qCustomer.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });
        return new PageResults<>(dtos, page, totalSize);
    }

    /**
     * 选中销售预订后，生成可进货明细（数量为剩余可转数量）
     */
    public List<PurchaseReservationItem> loadToPurchase(List<Long> reservationIds, Long merchantId) {
        if (CollectionUtils.isEmpty(reservationIds)) {
            return new ArrayList<>();
        }
        List<Tuple> rows = bqf.selectFrom(qSalesReservationItem)
                .select(qSalesReservationItem, qSalesReservation.orderNo, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qSalesReservation).on(qSalesReservation.id.eq(qSalesReservationItem.salesReservationId))
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesReservationItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesReservationItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .where(qSalesReservationItem.salesReservationId.in(reservationIds)
                        .and(qSalesReservationItem.merchantId.eq(merchantId))
                        .and(qSalesReservation.orderStatus.eq(OrderStatus.已审核))
                        .and(qSalesReservation.status.ne(2)))
                .orderBy(qSalesReservationItem.id.asc())
                .fetch();

        List<PurchaseReservationItem> result = new ArrayList<>();
        for (Tuple tuple : rows) {
            SalesReservationItem item = tuple.get(qSalesReservationItem);
            BigDecimal orderQty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal purchasedQty = item.getQuantityPurchased() == null ? BigDecimal.ZERO : item.getQuantityPurchased();
            BigDecimal remain = orderQty.subtract(purchasedQty);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            PurchaseReservationItem pItem = new PurchaseReservationItem();
            pItem.setProductId(item.getProductId());
            pItem.setBaseUnitId(item.getBaseUnitId());
            pItem.setQuantity(remain);
            pItem.setSecondaryUnitId(item.getSecondaryUnitId());
            pItem.setSecondaryQuantity(remain);
            pItem.setConversionRate(item.getConversionRate());
            pItem.setUnitPrice(item.getUnitPrice());
            pItem.setDiscountRate(item.getDiscountRate());
            pItem.setDiscountValue(item.getDiscountValue());
            pItem.setSubtotal(item.getSubtotal());
            pItem.setWarehouseId(item.getWarehouseId());
            pItem.setSalesReservationId(item.getSalesReservationId());
            pItem.setSalesReservationItemId(item.getId());
            pItem.setMerchantId(merchantId);
            pItem.setAccountBookId(item.getAccountBookId());
            result.add(pItem);
        }
        return result;
    }

    /**
     * 选中销售预订后，生成可销售订单明细（数量为剩余可转数量）
     */
    public List<SalesOrderItem> loadToSalesOrder(List<Long> reservationIds, Long merchantId) {
        if (CollectionUtils.isEmpty(reservationIds)) {
            return new ArrayList<>();
        }
        List<Tuple> rows = bqf.selectFrom(qSalesReservationItem)
                .select(qSalesReservationItem, qSalesReservation.orderNo, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qSalesReservation).on(qSalesReservation.id.eq(qSalesReservationItem.salesReservationId))
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesReservationItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesReservationItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .where(qSalesReservationItem.salesReservationId.in(reservationIds)
                        .and(qSalesReservationItem.merchantId.eq(merchantId))
                        .and(qSalesReservation.orderStatus.eq(OrderStatus.已审核))
                        .and(qSalesReservation.status.ne(2)))
                .orderBy(qSalesReservationItem.id.asc())
                .fetch();

        List<SalesOrderItem> result = new ArrayList<>();
        for (Tuple tuple : rows) {
            SalesReservationItem item = tuple.get(qSalesReservationItem);
            BigDecimal orderQty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal purchasedQty = item.getQuantityPurchased() == null ? BigDecimal.ZERO : item.getQuantityPurchased();
            BigDecimal remain = orderQty.subtract(purchasedQty);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            SalesOrderItem orderItem = new SalesOrderItem();
            orderItem.setProductId(item.getProductId());
            orderItem.setBaseUnitId(item.getBaseUnitId());
            orderItem.setQuantity(remain);
            orderItem.setSecondaryUnitId(item.getSecondaryUnitId());
            orderItem.setSecondaryQuantity(remain);
            orderItem.setConversionRate(item.getConversionRate());
            orderItem.setUnitPrice(item.getUnitPrice());
            orderItem.setDiscountRate(item.getDiscountRate());
            orderItem.setDiscountValue(item.getDiscountValue());
            orderItem.setSubtotal(item.getSubtotal());
            orderItem.setWarehouseId(item.getWarehouseId());
            orderItem.setMerchantId(merchantId);
            orderItem.setAccountBookId(item.getAccountBookId());
            result.add(orderItem);
        }
        return result;
    }

    public Map<String, BigDecimal> queryTotal(Query query) {
        BigDecimal amount = bqf.selectFrom(qSalesReservation)
                .select(qSalesReservation.finalAmount.sum())
                .where(query.builder).fetchFirst();
        BigDecimal quantity = bqf.selectFrom(qSalesReservationItem)
                .select(qSalesReservationItem.quantity.sum())
                .where(qSalesReservationItem.salesReservationId.in(
                        bqf.selectFrom(qSalesReservation).select(qSalesReservation.id).where(query.builder)
                )).fetchFirst();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", amount);
        result.put("quantity", java.util.Objects.requireNonNullElse(quantity, BigDecimal.ZERO));
        return result;
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSalesReservation.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSalesReservation.accountBookId, accountBookId);
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSalesReservation.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qSalesReservation.orderStatus.eq(state));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qSalesReservation.orderDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qSalesReservation.orderDate.loe(end));
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesReservation.customerId.eq(customerId));
            }
        }
    }
}
