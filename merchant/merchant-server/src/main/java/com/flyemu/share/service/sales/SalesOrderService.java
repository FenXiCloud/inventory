package com.flyemu.share.service.sales;

import com.flyemu.share.repository.basic.PriceRecordRepository;
import com.flyemu.share.repository.basic.WarehouseRepository;
import com.flyemu.share.repository.purchase.PurchaseInboundItemRepository;
import com.flyemu.share.repository.purchase.PurchaseInboundRepository;
import com.flyemu.share.repository.purchase.ToOrderLogRepository;
import com.flyemu.share.repository.sales.SalesOrderItemRepository;
import com.flyemu.share.repository.sales.SalesOrderRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.common.TenantAware;
import com.flyemu.share.common.UnitConvert;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.SalesOrderImportVo;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.sales.OrderTrackingDto;
import com.flyemu.share.dto.sales.PendingPurchaseItemDto;
import com.flyemu.share.dto.sales.PendingPurchaseOrderDto;
import com.flyemu.share.dto.sales.SalesOrderDto;
import com.flyemu.share.dto.sales.SalesOrderItemDto;
import com.flyemu.share.dto.sales.SalesOutboundItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.entity.purchase.PurchaseInboundItem;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseInboundItem;
import com.flyemu.share.entity.purchase.ToOrderLog;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.CreatePurchaseInboundForm;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.form.TransferToPurchaseForm;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.basic.ProductAuxiliaryUnitService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.flyemu.share.entity.sales.QSalesOutboundItem.salesOutboundItem;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesOrderService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = salesOutboundItem;
    private final SalesOutboundRepository salesOutboundRepository;

    private final static QSalesReturnItem qsalesReturnItem = QSalesReturnItem.salesReturnItem;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CodeSeedService codeSeedService;
    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final PriceRecordService priceRecordService;
    private final PriceRecordRepository priceRecordRepository;
    private final ProductAuxiliaryUnitService productAuxiliaryUnitService;
    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundItemRepository purchaseInboundItemRepository;
    private final ToOrderLogRepository toOrderLogRepository;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;
    private final static QAccountBookParameters qAccountBookParameters = QAccountBookParameters.accountBookParameters;

    public PageResults<SalesOrderDto> query(Page page, SalesOrderService.Query query) {
        long totalSize = bqf.selectFrom(qSalesOrder)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder, qCustomer.name, qMerchantUser.name)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                .where(query.builder)
                .orderBy(qSalesOrder.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesOrderDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOrderDto salesOrderDTO = BeanUtil.toBean(tuple.get(qSalesOrder), SalesOrderDto.class);
            salesOrderDTO.setCustomerName(tuple.get(qCustomer.name));
            salesOrderDTO.setCreatedName(tuple.get(qMerchantUser.name));
            //查询子表
            List<SalesOrderItem> salesOrderItemList = bqf.selectFrom(qSalesOrderItem)
                    .select(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(salesOrderDTO.getId()))
                    .fetch();
            List<SalesOrderItemDto> itemDTOs = new ArrayList<>();
            AtomicReference<BigDecimal> totalQuantity = new AtomicReference<>(BigDecimal.ZERO);
            salesOrderItemList.forEach(item -> {
                SalesOrderItemDto itemDTO = BeanUtil.toBean(item, SalesOrderItemDto.class);
                itemDTOs.add(itemDTO);
                BigDecimal quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v.add(quantity));

                //查询销售出库数量和退货数量
                List<Tuple> fetch = bqf.selectFrom(qSalesOutboundItem)
                        .leftJoin(qsalesReturnItem)
                        .on(qsalesReturnItem.salesOutboundId.eq(qSalesOutboundItem.salesOutboundId).and(qsalesReturnItem.outItemId.eq(qSalesOutboundItem.id)))
                        .select(qSalesOutboundItem.quantity, qsalesReturnItem.quantity)
                        .where(qSalesOutboundItem.salesOrderId.eq(item.getSalesOrderId())
                                .and(qSalesOutboundItem.tempId.eq(item.getId())))
                        .fetch();
                //出库数量
                BigDecimal outQuantity = fetch.stream()
                        .map(tuple1 -> java.util.Objects.requireNonNullElse(tuple1.get(qSalesOutboundItem.quantity), BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                //退货数量
                BigDecimal returnQuantity = fetch.stream()
                        .map(tuple2 -> java.util.Objects.requireNonNullElse(tuple2.get(qsalesReturnItem.quantity), BigDecimal.ZERO))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                itemDTO.setQuantityOut(outQuantity);
                itemDTO.setQuantityReturn(returnQuantity);
            });
            salesOrderDTO.setSalesOrderItemList(itemDTOs);
            salesOrderDTO.setTotalQuantity(totalQuantity);
            //关联查询出库单
            subQueryOutOrder(salesOrderDTO);
            //关联查询采购入库单
            subQueryPurchaseInbound(salesOrderDTO);
            salesOrderDTO.setPurchaseStatusText(getPurchaseStatusText(salesOrderDTO.getPurchaseStatus()));
            dtos.add(salesOrderDTO);
        });
        return new PageResults<>(dtos, page, totalSize);
    }

    private void subQueryOutOrder(SalesOrderDto salesOrderDTO) {
        //通过销售订单id 关联查询出销售出库单的所有商品
        List<SalesOutboundItem> salesOutboundItemList = bqf.selectFrom(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOrderId.eq(salesOrderDTO.getId()))
                .fetch();
        if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
            List<String> outOrderNoList = new ArrayList<>();
            //通过销售出库单id关联查询出销售出库单
            for (SalesOutboundItem item : salesOutboundItemList) {
                //只查询订单编号
                String outOrderNo = bqf.selectFrom(qSalesOutbound)
                        .select(qSalesOutbound.orderNo)
                        .where(qSalesOutbound.id.eq(item.getSalesOutboundId()))
                        .fetchOne();
                outOrderNoList.add(outOrderNo);
            }
            //将所有outOrderNo封装到list中，去重后返回
            salesOrderDTO.setOutOrderNo(outOrderNoList.stream().distinct().collect(Collectors.joining(",")));
            salesOrderDTO.setOutOrderNoList(outOrderNoList.stream().distinct().collect(Collectors.toList()));
        }
    }

    private void subQueryPurchaseInbound(SalesOrderDto salesOrderDTO) {
        // 通过sourceSalesOrderId查询关联的采购入库单
        List<PurchaseInbound> inbounds = bqf.selectFrom(qPurchaseInbound)
                .where(qPurchaseInbound.sourceSalesOrderId.eq(salesOrderDTO.getId())
                        .and(qPurchaseInbound.merchantId.eq(salesOrderDTO.getMerchantId())))
                .fetch();
        if (!CollectionUtils.isEmpty(inbounds)) {
            String orderNos = inbounds.stream()
                    .map(PurchaseInbound::getOrderNo)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.joining(","));
            salesOrderDTO.setPurchaseInOrderNos(orderNos);
        }
    }

    @Transactional
    public SalesOrder save(SalesOrderForm salesOrderForm, Long merchantId) {
        SalesOrder salesOrder = salesOrderForm.getSalesOrder();
        checkoutService.assertEditable(salesOrder.getMerchantId(), salesOrder.getAccountBookId(), salesOrder.getOrderDate());
        salesOrder.setMerchantId(merchantId);
        assertCreditLimit(salesOrder.getCustomerId(), salesOrder.getFinalAmount(), salesOrder.getMerchantId(), salesOrder.getAccountBookId());
        Long id = salesOrder.getId();
        List<SalesOrderItem> salesOrderItemList = salesOrderForm.getSalesOrderItemList();
        if (id != null) {
            //查询
            SalesOrder original = bqf.selectFrom(qSalesOrder)
                    .where(qSalesOrder.id.eq(id).and(qSalesOrder.merchantId.eq(merchantId)))
                    .fetchFirst();
            if (original == null) {
                throw new ServiceException("单据不存在");
            }
            //已审核单据不能修改
            OrderStatus orderStatus = original.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已审核)) {
                throw new InvalidContextException("已审核单据不能修改");
            }
            BeanUtil.copyProperties(salesOrder, original, CopyOptions.create().ignoreNullValue());
            //修改销售订单
            SalesOrder update = salesOrderRepository.save(original);
            //清除销售订单商品
            jqf.delete(qSalesOrderItem).where(qSalesOrderItem.salesOrderId.eq(id)).execute();
            //保存新关系
            if (!CollectionUtils.isEmpty(salesOrderItemList)) {
                salesOrderItemList.forEach(item -> {
                    //服务端权威换算：基本数量 = 销售数量 × 换算率；基本单价 = 销售单价 ÷ 换算率
                    normalizeUnit(item);
                    //保存价格记录
                    savePrice(item, update);
                    item.setSalesOrderId(update.getId());
                    item.setAccountBookId(salesOrder.getAccountBookId());
                    item.setMerchantId(merchantId);
                });
                //批量修改销售订单商品
                salesOrderItemRepository.saveAll(salesOrderItemList);
            }
            return update;
        } else {
            //销售订单状态初始化
            salesOrder.setOrderStatus(OrderStatus.已保存);
            //初始化订单状态;
            salesOrder.setStatus(0);
            //销售订单编号
            salesOrder.setOrderNo(codeSeedService.generateCode(merchantId, salesOrder.getAccountBookId(), "销售订单"));
            //保存销售订单
            SalesOrder save = salesOrderRepository.save(salesOrder);
            if (!CollectionUtils.isEmpty(salesOrderItemList)) {
                salesOrderItemList.forEach(item -> {
                    //服务端权威换算：基本数量 = 销售数量 × 换算率；基本单价 = 销售单价 ÷ 换算率
                    normalizeUnit(item);
                    //保存价格记录
                    savePrice(item, save);
                    item.setSalesOrderId(save.getId());
                    item.setAccountBookId(salesOrder.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setCreatedBy(salesOrder.getCreatedBy());
                    item.setCreatedAt(salesOrder.getCreatedAt());
                    //初始化出库数量
                    item.setQuantityOut(BigDecimal.ZERO);
                    //初始化退货数量
                    item.setQuantityReturn(BigDecimal.ZERO);
                });
                //批量添加销售订单商品
                salesOrderItemRepository.saveAll(salesOrderItemList);
            }
            return save;
        }
    }

    /**
     * 老数据兜底 + 服务端权威换算（保存时调用）：
     * 基本数量 = 销售数量 × 换算率；基本单价 = 销售单价 ÷ 换算率。
     */
    private void normalizeUnit(SalesOrderItem d) {
        if (d.getConversionRate() == null) {
            d.setConversionRate(BigDecimal.ONE);
        }
        if (d.getSecondaryUnitId() == null) {
            d.setSecondaryUnitId(d.getBaseUnitId());
        }
        if (d.getSecondaryQuantity() == null) {
            d.setSecondaryQuantity(d.getQuantity());
        }
        if (d.getSecondaryPrice() == null) {
            d.setSecondaryPrice(UnitConvert.secondaryPrice(d.getUnitPrice(), d.getConversionRate()));
        }
        d.setQuantity(UnitConvert.toBaseQty(d.getSecondaryQuantity(), d.getConversionRate()));
        d.setUnitPrice(UnitConvert.unitPrice(d.getSecondaryPrice(), d.getConversionRate()));
    }

    /** load/详情回填：老数据兜底业务单位字段（不重算已持久化的基本数量/基本单价） */
    private void fillUnitFallback(SalesOrderItemDto d) {
        if (d.getConversionRate() == null) {
            d.setConversionRate(BigDecimal.ONE);
        }
        if (d.getSecondaryUnitId() == null) {
            d.setSecondaryUnitId(d.getBaseUnitId());
        }
        if (d.getSecondaryQuantity() == null) {
            d.setSecondaryQuantity(d.getQuantity());
        }
        if (d.getSecondaryPrice() == null) {
            d.setSecondaryPrice(UnitConvert.secondaryPrice(d.getUnitPrice(), d.getConversionRate()));
        }
    }

    private void savePrice(SalesOrderItem item, SalesOrder salesOrder) {
        //保存价格记录
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setOrderId(salesOrder.getId());
        priceRecord.setUnitPrice(item.getUnitPrice());
        priceRecord.setBaseUnitId(item.getBaseUnitId());
        priceRecord.setProductId(item.getProductId());
        priceRecord.setMerchantId(salesOrder.getMerchantId());
        priceRecord.setAccountBookId(salesOrder.getAccountBookId());
        priceRecord.setCustomerId(salesOrder.getCustomerId());
        priceRecord.setPriceSource(PriceSource.最近销售价格);
        priceRecord.setPriceType(PriceType.最近销售价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    /**
     * 信用额度校验：客户应收余额 + 本单金额 超过信用额度时拦截。
     * creditLimit 为 null 视为不限制。
     */
    private void assertCreditLimit(Long customerId, BigDecimal amount, Long merchantId, Long accountBookId) {
        if (customerId == null) {
            return;
        }
        Customer customer = bqf.selectFrom(qCustomer)
                .where(qCustomer.id.eq(customerId)
                        .and(qCustomer.merchantId.eq(merchantId))
                        .and(qCustomer.accountBookId.eq(accountBookId)))
                .fetchOne();
        if (customer == null || customer.getCreditLimit() == null) {
            return;
        }
        BigDecimal balance = customer.getBalance() == null ? BigDecimal.ZERO : customer.getBalance();
        BigDecimal orderAmount = amount == null ? BigDecimal.ZERO : amount;
        if (balance.add(orderAmount).compareTo(customer.getCreditLimit()) > 0) {
            throw new ServiceException("客户「" + customer.getName() + "」超出信用额度：应收余额 " + balance
                    + " + 本单金额 " + orderAmount + " > 信用额度 " + customer.getCreditLimit());
        }
    }

    @Transactional
    public void delete(Long salesOrderId, Long merchantId, Long accountBookId) {
        SalesOrder original = salesOrderRepository.getById(salesOrderId);

        // 结账日期校验：已结账的单据不能删除
        checkoutService.assertEditable(original.getMerchantId(), original.getAccountBookId(), original.getOrderDate());

        //已审核单据不能删除
        OrderStatus orderStatus = original.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已审核)) {
            throw new InvalidContextException("已审核单据不能删除");
        }
        //根据销售单id查询销售出库单商品
        List<SalesOutboundItem> salesOutboundItemList = bqf.selectFrom(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOrderId.eq(salesOrderId))
                .fetch();
        if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
            throw new InvalidContextException("已关联销售出库单不能删除");
        }

        //删除销售订单
        jqf.delete(qSalesOrder)
                .where(qSalesOrder.id.eq(salesOrderId).and(qSalesOrder.merchantId.eq(merchantId)).and(qSalesOrder.accountBookId.eq(accountBookId)))
                .execute();

        //删除销售订单商品
        jqf.delete(qSalesOrderItem)
                .where(qSalesOrderItem.salesOrderId.eq(salesOrderId).and(qSalesOrderItem.merchantId.eq(merchantId)).and(qSalesOrderItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SalesOrder> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSalesOrder).where(qSalesOrder.merchantId.eq(merchantId).and(qSalesOrder.accountBookId.eq(accountBookId))).fetch();
    }

    public SalesOrderDto load(Long merchantId, Long orderId) {
        SalesOrder salesOrder = bqf.selectFrom(qSalesOrder)
                .where(qSalesOrder.merchantId.eq(merchantId).and(qSalesOrder.id.eq(orderId)))
                .fetchFirst();
        if (salesOrder == null) {
            throw new ServiceException("单据不存在");
        }
        SalesOrderDto dto = BeanUtil.toBean(salesOrder, SalesOrderDto.class);
        QUnit qUnitSec = new QUnit("unitSec");
        List<Tuple> fetch = jqf.selectFrom(qSalesOrderItem)
                .select(qSalesOrderItem, qProduct.code, qProduct.name, qUnit.name, qUnitSec.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOrderItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOrderItem.baseUnitId))
                .leftJoin(qUnitSec).on(qUnitSec.id.eq(qSalesOrderItem.secondaryUnitId))
                .where(qSalesOrderItem.salesOrderId.eq(orderId)
                        .and(qSalesOrderItem.merchantId.eq(merchantId)))
                .orderBy(qSalesOrderItem.id.asc()).fetch();
        List<SalesOrderItemDto> salesOrderItemDTOS = new ArrayList<>();
        Set<Long> productIds = new HashSet<>();
        fetch.forEach(tuple -> {
            SalesOrderItemDto salesOrderItemDTO = BeanUtil.toBean(tuple.get(qSalesOrderItem), SalesOrderItemDto.class);
            salesOrderItemDTO.setProductName(tuple.get(qProduct.name));
            salesOrderItemDTO.setProductCode(tuple.get(qProduct.code));
            salesOrderItemDTO.setUnitName(tuple.get(qUnit.name));
            //老数据兜底业务单位字段 + 业务单位名（旧行无业务单位则回落基本单位）
            fillUnitFallback(salesOrderItemDTO);
            String secondaryName = tuple.get(qUnitSec.name);
            if (salesOrderItemDTO.getSecondaryUnitId() != null && StrUtil.isNotBlank(secondaryName)) {
                salesOrderItemDTO.setSecondaryUnitName(secondaryName);
            } else {
                salesOrderItemDTO.setSecondaryUnitName(tuple.get(qUnit.name));
            }
            if (salesOrderItemDTO.getProductId() != null) {
                productIds.add(salesOrderItemDTO.getProductId());
            }
            salesOrderItemDTOS.add(salesOrderItemDTO);
        });
        // 回填商品可用单位列表（基本单位在前，unitPrice=该行基本单价），订单行"销售单位"下拉可切换
        Map<Long, List<AuxiliaryUnitPrice>> unitMap = productAuxiliaryUnitService.loadAuxUnits(productIds, merchantId);
        for (SalesOrderItemDto row : salesOrderItemDTOS) {
            row.setAuxiliaryUnitPrices(ProductAuxiliaryUnitService.withBase(
                    row.getBaseUnitId(), row.getUnitName(), row.getUnitPrice(), unitMap.get(row.getProductId())));
        }
        dto.setSalesOrderItemList(salesOrderItemDTOS);
        dto.setPurchaseStatusText(getPurchaseStatusText(dto.getPurchaseStatus()));
        return dto;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择单据");
        }
        List<SalesOrder> salesOrders = bqf.selectFrom(qSalesOrder)
                .where(qSalesOrder.merchantId.eq(merchantId).and(qSalesOrder.id.in(ids)))
                .fetch();
        if (salesOrders.isEmpty()) {
            throw new ServiceException("未找到数据~");
        }

        // 结账日期校验：已结账的单据不能审核/反审核
        for (SalesOrder order : salesOrders) {
            checkoutService.assertEditable(order.getMerchantId(), order.getAccountBookId(), order.getOrderDate());
        }

        salesOrders.forEach(order -> {
            if (OrderStatus.已保存.equals(state)) {
                List<SalesOutboundItem> salesOutboundItemList = bqf.selectFrom(qSalesOutboundItem)
                        .where(qSalesOutboundItem.salesOrderId.eq(order.getId()))
                        .fetch();
                if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                    throw new InvalidContextException("已关联销售出库单不能反审核");
                }
            }
            if (OrderStatus.已取消.equals(state)) {
                if (OrderStatus.已审核.equals(order.getOrderStatus())) {
                    throw new InvalidContextException("已审核单据不能取消");
                }
            }
            if (OrderStatus.已审核.equals(state)) {
                assertCreditLimit(order.getCustomerId(), order.getFinalAmount(), merchantId, order.getAccountBookId());
            }
            order.setOrderStatus(state);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(adminId);
        });

        salesOrderRepository.saveAll(salesOrders);
    }

    public Map<String, BigDecimal> queryTotal(Query query) {
        BigDecimal amount = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder.finalAmount.sum())
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                .where(query.builder).fetchFirst();
        // 子查询统计所有符合条件的订单的商品数量总和，避免 JOIN 导致金额翻倍
        BigDecimal quantity = bqf.selectFrom(qSalesOrderItem)
                .select(qSalesOrderItem.quantity.sum())
                .where(qSalesOrderItem.salesOrderId.in(
                        bqf.selectFrom(qSalesOrder).select(qSalesOrder.id)
                                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                                .where(query.builder)
                )).fetchFirst();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", amount);
        result.put("quantity", java.util.Objects.requireNonNullElse(quantity, BigDecimal.ZERO));
        return result;
    }

    /**
     * 销售出库选源单列表：已审核且未全部出库的销售订单
     */
    public PageResults<SalesOrderDto> queryToOutBound(Page page, Query query) {
        BooleanBuilder builder = query.builder.and(qSalesOrder.status.ne(2));
        long totalSize = bqf.selectFrom(qSalesOrder)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .where(builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder, qCustomer.name, qMerchantUser.name)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                .where(builder)
                .orderBy(qSalesOrder.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesOrderDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOrderDto dto = BeanUtil.toBean(tuple.get(qSalesOrder), SalesOrderDto.class);
            dto.setCustomerName(tuple.get(qCustomer.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            subQueryOutOrder(dto);
            dto.setPurchaseStatusText(getPurchaseStatusText(dto.getPurchaseStatus()));
            dtos.add(dto);
        });
        fillOutBoundQuantity(dtos);
        return new PageResults<>(dtos, page, totalSize);
    }

    /**
     * 选源单列表：按订单头汇总 商品数量 / 已出库 / 已退货 / 可出库
     * 口径与 {@link #calcOutAndReturnQuantity} 及 {@link #loadToOutbound} 完全一致：
     * 可出库 = 逐明细 (订单数量 + 已退货 - 已出库) 取正后求和，避免跨行正负抵消。
     */
    private void fillOutBoundQuantity(List<SalesOrderDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return;
        }
        List<Long> orderIds = new ArrayList<>();
        for (SalesOrderDto dto : dtos) {
            if (dto.getId() != null) {
                orderIds.add(dto.getId());
            }
        }
        if (orderIds.isEmpty()) {
            return;
        }

        // 订单明细数量：orderItemId -> (订单id, 数量)
        Map<Long, Long> orderIdByItem = new HashMap<>();
        Map<Long, BigDecimal> orderQtyByItem = new HashMap<>();
        List<Tuple> items = bqf.select(qSalesOrderItem.salesOrderId, qSalesOrderItem.id, qSalesOrderItem.quantity)
                .from(qSalesOrderItem)
                .where(qSalesOrderItem.salesOrderId.in(orderIds))
                .fetch();
        for (Tuple tuple : items) {
            Long itemId = tuple.get(qSalesOrderItem.id);
            Long salesOrderId = tuple.get(qSalesOrderItem.salesOrderId);
            if (itemId == null || salesOrderId == null) {
                continue;
            }
            orderIdByItem.put(itemId, salesOrderId);
            orderQtyByItem.put(itemId, tuple.get(qSalesOrderItem.quantity) == null
                    ? BigDecimal.ZERO : tuple.get(qSalesOrderItem.quantity));
        }

        // 出库明细 + 关联退货明细（行语义与 calcOutAndReturnQuantity 相同：一条出库行配多条退货行时，出库量随行重复）
        Map<Long, BigDecimal> outByItem = new HashMap<>();
        Map<Long, BigDecimal> returnByItem = new HashMap<>();
        List<Tuple> rows = bqf.select(qSalesOutboundItem.tempId, qSalesOutboundItem.quantity, qsalesReturnItem.quantity)
                .from(qSalesOutboundItem)
                .leftJoin(qsalesReturnItem)
                .on(qsalesReturnItem.salesOutboundId.eq(qSalesOutboundItem.salesOutboundId)
                        .and(qsalesReturnItem.outItemId.eq(qSalesOutboundItem.id)))
                .where(qSalesOutboundItem.salesOrderId.in(orderIds))
                .fetch();
        for (Tuple tuple : rows) {
            Long itemId = tuple.get(qSalesOutboundItem.tempId);
            if (itemId == null || !orderIdByItem.containsKey(itemId)) {
                continue;
            }
            BigDecimal out = tuple.get(qSalesOutboundItem.quantity) == null
                    ? BigDecimal.ZERO : tuple.get(qSalesOutboundItem.quantity);
            BigDecimal ret = tuple.get(qsalesReturnItem.quantity) == null
                    ? BigDecimal.ZERO : tuple.get(qsalesReturnItem.quantity);
            outByItem.merge(itemId, out, BigDecimal::add);
            returnByItem.merge(itemId, ret, BigDecimal::add);
        }

        // 按订单汇总
        Map<Long, BigDecimal[]> agg = new HashMap<>(); // [商品数量, 已出库, 已退货, 可出库]
        for (Map.Entry<Long, Long> entry : orderIdByItem.entrySet()) {
            Long itemId = entry.getKey();
            Long salesOrderId = entry.getValue();
            BigDecimal orderQty = orderQtyByItem.getOrDefault(itemId, BigDecimal.ZERO);
            BigDecimal outQty = outByItem.getOrDefault(itemId, BigDecimal.ZERO);
            BigDecimal returnQty = returnByItem.getOrDefault(itemId, BigDecimal.ZERO);
            BigDecimal remain = orderQty.add(returnQty).subtract(outQty);
            BigDecimal[] a = agg.computeIfAbsent(salesOrderId, k -> new BigDecimal[4]);
            a[0] = nvl(a[0]).add(orderQty);
            a[1] = nvl(a[1]).add(outQty);
            a[2] = nvl(a[2]).add(returnQty);
            if (remain.compareTo(BigDecimal.ZERO) > 0) {
                a[3] = nvl(a[3]).add(remain);
            }
        }
        for (SalesOrderDto dto : dtos) {
            BigDecimal[] a = agg.get(dto.getId());
            dto.setOrderQuantity(a == null ? BigDecimal.ZERO : nvl(a[0]));
            dto.setOutQuantity(a == null ? BigDecimal.ZERO : nvl(a[1]));
            dto.setReturnQuantity(a == null ? BigDecimal.ZERO : nvl(a[2]));
            dto.setRemainQuantity(a == null ? BigDecimal.ZERO : nvl(a[3]));
        }
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /**
     * 选中销售订单后，生成可出库明细（数量为剩余可出库基本数量）。
     * 业务单位按订单单位启发折算：剩余基本量能被换算率整除则维持订单"销售单位"，否则回落到基本单位。
     */
    public List<SalesOutboundItemDto> loadToOutbound(List<Long> orderIds, Long merchantId, Long customerId) {
        if (CollectionUtils.isEmpty(orderIds)) {
            return new ArrayList<>();
        }
        QUnit qUnitSec = new QUnit("unitSec");
        List<Tuple> rows = bqf.selectFrom(qSalesOrderItem)
                .select(qSalesOrderItem, qSalesOrder.orderNo, qProduct.code, qProduct.name, qUnit.name, qUnitSec.name)
                .leftJoin(qSalesOrder).on(qSalesOrder.id.eq(qSalesOrderItem.salesOrderId))
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOrderItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOrderItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnitSec).on(qUnitSec.id.eq(qSalesOrderItem.secondaryUnitId).and(qUnitSec.merchantId.eq(merchantId)))
                .where(qSalesOrderItem.salesOrderId.in(orderIds)
                        .and(qSalesOrderItem.merchantId.eq(merchantId))
                        .and(qSalesOrder.customerId.eq(customerId))
                        .and(qSalesOrder.orderStatus.eq(OrderStatus.已审核))
                        .and(qSalesOrder.status.ne(2)))
                .orderBy(qSalesOrderItem.id.asc())
                .fetch();

        List<SalesOutboundItemDto> result = new ArrayList<>();
        Set<Long> productIds = new HashSet<>();
        for (Tuple tuple : rows) {
            SalesOrderItem item = tuple.get(qSalesOrderItem);
            BigDecimal[] outAndReturn = calcOutAndReturnQuantity(item.getSalesOrderId(), item.getId());
            BigDecimal orderQty = item.getQuantity() == null ? BigDecimal.ZERO : item.getQuantity();
            BigDecimal remain = orderQty.add(outAndReturn[1]).subtract(outAndReturn[0]);
            if (remain.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            SalesOutboundItemDto dto = BeanUtil.toBean(item, SalesOutboundItemDto.class);
            dto.setTempId(item.getId());
            dto.setId(null);
            dto.setSalesOrderId(item.getSalesOrderId());
            dto.setSalesOrderNo(tuple.get(qSalesOrder.orderNo));
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dto.setUnitName(tuple.get(qUnit.name));
            dto.setQuantity(remain);
            // 默认单位启发：剩余基本数量能被换算率整除 → 维持订单"销售单位"；否则回落到基本单位
            BigDecimal rate = UnitConvert.rate(item.getConversionRate());
            boolean keepOrderUnit = item.getSecondaryUnitId() != null && UnitConvert.isWholeSecondary(remain, rate);
            if (keepOrderUnit) {
                dto.setSecondaryUnitId(item.getSecondaryUnitId());
                dto.setSecondaryUnitName(tuple.get(qUnitSec.name));
                dto.setConversionRate(rate);
                dto.setSecondaryQuantity(UnitConvert.toSecondaryQty(remain, rate));
                dto.setSecondaryPrice(item.getSecondaryPrice() != null ? item.getSecondaryPrice()
                        : UnitConvert.secondaryPrice(item.getUnitPrice(), rate));
            } else {
                dto.setSecondaryUnitId(item.getBaseUnitId());
                dto.setSecondaryUnitName(tuple.get(qUnit.name));
                dto.setConversionRate(BigDecimal.ONE);
                dto.setSecondaryQuantity(remain);
                dto.setSecondaryPrice(UnitConvert.nvl(item.getUnitPrice()));
            }
            // 金额沿用基本口径：小计/折扣 = 基本数量 × 基本单价（业务量×业务价恒等）
            BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
            BigDecimal discountRate = item.getDiscountRate() == null ? BigDecimal.ZERO : item.getDiscountRate();
            BigDecimal qty = remain;
            BigDecimal discountValue = unitPrice.multiply(qty).multiply(discountRate)
                    .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal subtotal = unitPrice.multiply(qty).subtract(discountValue)
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            dto.setDiscountValue(discountValue);
            dto.setSubtotal(subtotal);
            if (dto.getProductId() != null) {
                productIds.add(dto.getProductId());
            }
            result.add(dto);
        }
        // 回填商品可用单位列表（基本单位在前，unitPrice=该行基本单价），出库行"销售单位"下拉可切换
        Map<Long, List<AuxiliaryUnitPrice>> unitMap = productAuxiliaryUnitService.loadAuxUnits(productIds, merchantId);
        for (SalesOutboundItemDto dto : result) {
            dto.setAuxiliaryUnitPrices(ProductAuxiliaryUnitService.withBase(
                    dto.getBaseUnitId(), dto.getUnitName(), dto.getUnitPrice(), unitMap.get(dto.getProductId())));
        }
        return result;
    }

    /** @return [outQuantity, returnQuantity] */
    private BigDecimal[] calcOutAndReturnQuantity(Long salesOrderId, Long orderItemId) {
        List<Tuple> fetch = bqf.selectFrom(qSalesOutboundItem)
                .leftJoin(qsalesReturnItem)
                .on(qsalesReturnItem.salesOutboundId.eq(qSalesOutboundItem.salesOutboundId)
                        .and(qsalesReturnItem.outItemId.eq(qSalesOutboundItem.id)))
                .select(qSalesOutboundItem.quantity, qsalesReturnItem.quantity)
                .where(qSalesOutboundItem.salesOrderId.eq(salesOrderId)
                        .and(qSalesOutboundItem.tempId.eq(orderItemId)))
                .fetch();
        BigDecimal outQuantity = fetch.stream()
                .map(t -> java.util.Objects.requireNonNullElse(t.get(qSalesOutboundItem.quantity), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal returnQuantity = fetch.stream()
                .map(t -> java.util.Objects.requireNonNullElse(t.get(qsalesReturnItem.quantity), BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new BigDecimal[]{outQuantity, returnQuantity};
    }

    /**
     * 销售订单转采购入库
     * 输入：salesOrderId + supplierId + 采购明细（商品、数量、单价、仓库）+ 预计到货日期/备注/自动审核
     * 处理：校验 → 读取系统参数（默认单据状态/是否自动审核/是否允许分批） → 生成采购入库单 → 写操作日志 → 更新销售订单采购状态
     * 输出：采购入库单ID
     */
    @Transactional
    public Long transferToPurchaseInbound(Long salesOrderId, Long supplierId,
                                           List<PurchaseInboundItem> items,
                                           LocalDate expectedDeliveryDate, String remark, Boolean autoAudit, String orderStatus,
                                           Long merchantId, Long adminId, Long accountBookId) {
        if (salesOrderId == null) throw new ServiceException("销售订单ID不能为空");
        if (supplierId == null) throw new ServiceException("请选择供货商");
        if (CollectionUtils.isEmpty(items)) throw new ServiceException("采购明细不能为空");

        // 0. 结账日期校验：采购入库单日期不能早于或等于结账日期
        checkoutService.assertEditable(merchantId, accountBookId, LocalDate.now());

        // 1. 校验销售订单
        SalesOrder salesOrder = bqf.selectFrom(qSalesOrder)
                .where(qSalesOrder.id.eq(salesOrderId).and(qSalesOrder.merchantId.eq(merchantId)))
                .fetchFirst();
        if (salesOrder == null) throw new ServiceException("销售订单不存在");
        if (!OrderStatus.已审核.equals(salesOrder.getOrderStatus())) {
            throw new ServiceException("销售订单未审核，不能转采购");
        }
        if (Boolean.TRUE.equals(salesOrder.getClosed())) {
            throw new ServiceException("销售订单已关闭，不能转采购");
        }
        if (salesOrder.getPurchaseStatus() != null && salesOrder.getPurchaseStatus() >= 2) {
            throw new ServiceException("该销售订单已全部采购");
        }

        // 1.1 读取系统参数（以销定购默认单据状态/是否自动审核/是否允许分批采购）
        AccountBookParameters params = bqf.selectFrom(qAccountBookParameters)
                .where(qAccountBookParameters.accountBookId.eq(Math.toIntExact(accountBookId)))
                .fetchFirst();
        String defaultStatus = StrUtil.isNotBlank(orderStatus) ? orderStatus
                : (params != null && StrUtil.isNotBlank(params.getToOrderDefaultStatus())
                ? params.getToOrderDefaultStatus() : "待审核");
        boolean sysAutoAudit = params != null && Boolean.TRUE.equals(params.getToOrderAutoAudit());
        boolean autoAuditFlag = autoAudit != null ? autoAudit : sysAutoAudit;

        // 生成状态：仅"已审核"直接置为已审核；"草稿"/"待审核"/其他 一律生成"已保存"（草稿），
        // 与采购入库单列表的编辑/审核操作（仅认"已保存"）保持一致，避免生成后无法操作
        OrderStatus targetStatus;
        if ("已审核".equals(defaultStatus)) {
            targetStatus = OrderStatus.已审核;
        } else {
            targetStatus = OrderStatus.已保存;
        }
        if (autoAuditFlag) {
            targetStatus = OrderStatus.已审核;
        }

        // 1.2 限制采购总量：允许分批采购，但每个商品的累计采购数量不得超过待采购数量
        for (PurchaseInboundItem item : items) {
            if (item.getProductId() == null) continue;
            BigDecimal pendingPurchase = calcOrderProductPendingPurchase(salesOrderId, item.getProductId(), merchantId);
            BigDecimal qty = item.getSecondaryQuantity() != null ? item.getSecondaryQuantity() : BigDecimal.ZERO;
            if (qty.compareTo(pendingPurchase) > 0) {
                throw new ServiceException("采购数量超过待采购数量（剩余可采购：" + pendingPurchase + "）");
            }
        }

        // 2. 生成采购入库单
        PurchaseInbound inbound = new PurchaseInbound();
        inbound.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "采购入库单"));
        inbound.setSupplierId(supplierId);
        inbound.setInboundDate(LocalDate.now());
        inbound.setExpectedDeliveryDate(expectedDeliveryDate);
        inbound.setRemarks(remark);
        inbound.setSourceSalesOrderId(salesOrderId);
        inbound.setSourceType("以销定购");
        inbound.setOrderStatus(targetStatus);
        if (OrderStatus.已审核.equals(targetStatus)) {
            inbound.setApprovedBy(adminId);
            inbound.setApprovedAt(LocalDateTime.now());
        }
        inbound.setCreatedBy(adminId);
        inbound.setCreatedAt(LocalDateTime.now());
        inbound.setMerchantId(merchantId);
        inbound.setAccountBookId(accountBookId);

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal secondarySum = BigDecimal.ZERO;

        // 3. 保存明细
        PurchaseInbound savedInbound = purchaseInboundRepository.save(inbound);
        for (PurchaseInboundItem item : items) {
            if (item.getProductId() == null) throw new ServiceException("明细产品不能为空");
            if (item.getWarehouseId() == null) throw new ServiceException("明细仓库不能为空");
            if (item.getSecondaryQuantity() == null || item.getSecondaryQuantity().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("采购数量必须大于0");
            }

            BigDecimal conversionRate = item.getConversionRate() != null ? item.getConversionRate() : BigDecimal.ONE;
            BigDecimal secondaryPrice = item.getSecondaryPrice() != null ? item.getSecondaryPrice() : BigDecimal.ZERO;
            BigDecimal quantity = item.getSecondaryQuantity().multiply(conversionRate);
            BigDecimal subtotal = secondaryPrice.multiply(item.getSecondaryQuantity());

            item.setPurchaseInboundId(savedInbound.getId());
            item.setUnitPrice(secondaryPrice.divide(conversionRate, 2, java.math.RoundingMode.HALF_UP));
            item.setQuantity(quantity);
            item.setSubtotal(subtotal);
            item.setReturnQuantity(item.getSecondaryQuantity());
            item.setDiscountRate(item.getDiscountRate() != null ? item.getDiscountRate() : BigDecimal.ZERO);
            item.setDiscountAmount(item.getDiscountAmount() != null ? item.getDiscountAmount() : BigDecimal.ZERO);
            item.setCreatedBy(adminId);
            item.setCreatedAt(LocalDateTime.now());
            item.setMerchantId(merchantId);
            item.setAccountBookId(accountBookId);

            totalAmount = totalAmount.add(subtotal);
            secondarySum = secondarySum.add(item.getSecondaryQuantity());
        }
        purchaseInboundItemRepository.saveAll(items);

        // 更新采购入库单金额汇总（折扣默认 0，避免订单头折扣字段为空影响列表展示/编辑）
        savedInbound.setDiscountRate(BigDecimal.ZERO);
        savedInbound.setDiscountAmount(BigDecimal.ZERO);
        savedInbound.setTotalAmount(totalAmount);
        savedInbound.setFinalAmount(totalAmount);
        savedInbound.setSecondarySum(secondarySum);
        savedInbound.setReturnSum(secondarySum);
        purchaseInboundRepository.save(savedInbound);

        // 4. 更新销售订单采购状态
        updateSalesOrderPurchaseStatus(salesOrderId, merchantId);

        // 5. 追加关联采购入库单ID到销售订单
        appendPurchaseInboundToSalesOrder(salesOrderId, savedInbound.getId(), merchantId);

        // 6. 写以销定购操作日志
        List<ToOrderLog> logs = new ArrayList<>();
        for (PurchaseInboundItem item : items) {
            ToOrderLog log = new ToOrderLog();
            log.setSaleOrderId(salesOrderId);
            log.setPurchaseInId(savedInbound.getId());
            log.setGoodsId(item.getProductId());
            log.setPurchaseQuantity(item.getSecondaryQuantity());
            log.setPurchasePrice(item.getSecondaryPrice());
            log.setSupplierId(supplierId);
            log.setCreatedBy(adminId);
            log.setMerchantId(merchantId);
            log.setAccountBookId(accountBookId);
            logs.add(log);
        }
        toOrderLogRepository.saveAll(logs);

        return savedInbound.getId();
    }

    /**
     * 原签名兼容方法（无预计到货日期/备注/自动审核）
     */
    @Transactional
    public Long transferToPurchaseInbound(Long salesOrderId, Long supplierId,
                                           List<PurchaseInboundItem> items,
                                           Long merchantId, Long adminId, Long accountBookId) {
        return transferToPurchaseInbound(salesOrderId, supplierId, items, null, null, null, null,
                merchantId, adminId, accountBookId);
    }

    /**
     * 追加采购入库单ID到销售订单的 purchaseInIds（JSON数组）
     */
    private void appendPurchaseInboundToSalesOrder(Long salesOrderId, Long purchaseInId, Long merchantId) {
        SalesOrder salesOrder = bqf.selectFrom(qSalesOrder)
                .where(qSalesOrder.id.eq(salesOrderId).and(qSalesOrder.merchantId.eq(merchantId)))
                .fetchFirst();
        if (salesOrder == null) return;
        List<Long> ids = new ArrayList<>();
        if (StrUtil.isNotBlank(salesOrder.getPurchaseInIds())) {
            try {
                ids = JSONUtil.toList(salesOrder.getPurchaseInIds(), Long.class);
            } catch (Exception ignored) {
                ids = new ArrayList<>();
            }
            if (ids == null) ids = new ArrayList<>();
        }
        if (!ids.contains(purchaseInId)) {
            ids.add(purchaseInId);
        }
        jqf.update(qSalesOrder)
                .set(qSalesOrder.purchaseInIds, JSONUtil.toJsonStr(ids))
                .where(qSalesOrder.id.eq(salesOrderId))
                .execute();
    }

    /**
     * 计算某销售订单中某商品的待采购数量（未出库数量 - 已采购数量）
     */
    private BigDecimal calcOrderProductPendingPurchase(Long salesOrderId, Long productId, Long merchantId) {
        SalesOrderItem orderItem = bqf.selectFrom(qSalesOrderItem)
                .where(qSalesOrderItem.salesOrderId.eq(salesOrderId)
                        .and(qSalesOrderItem.productId.eq(productId))
                        .and(qSalesOrderItem.merchantId.eq(merchantId)))
                .fetchFirst();
        if (orderItem == null) return BigDecimal.ZERO;
        BigDecimal orderQty = orderItem.getQuantity() != null ? orderItem.getQuantity() : BigDecimal.ZERO;
        BigDecimal[] outAndReturn = calcOutAndReturnQuantity(salesOrderId, orderItem.getId());
        BigDecimal pendingOutbound = orderQty.add(outAndReturn[1]).subtract(outAndReturn[0]);
        BigDecimal purchased = calcPurchasedQuantity(salesOrderId, productId, merchantId);
        return pendingOutbound.subtract(purchased);
    }

    /**
     * 更新销售订单的采购状态：根据所有关联采购入库单的已入库数量 vs 销售订单数量
     */
    private void updateSalesOrderPurchaseStatus(Long salesOrderId, Long merchantId) {
        SalesOrder salesOrder = bqf.selectFrom(qSalesOrder)
                .where(qSalesOrder.id.eq(salesOrderId).and(qSalesOrder.merchantId.eq(merchantId)))
                .fetchFirst();
        if (salesOrder == null) return;

        // 查询该销售订单的所有已审核采购入库单的明细数量（按商品汇总）
        List<PurchaseInboundItem> allInboundItems = bqf.selectFrom(qPurchaseInboundItem)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .where(qPurchaseInbound.sourceSalesOrderId.eq(salesOrderId)
                        .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核))
                        .and(qPurchaseInboundItem.merchantId.eq(merchantId)))
                .fetch();

        // 按 productId 汇总已采购数量
        Map<Long, BigDecimal> purchasedByProduct = new HashMap<>();
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
            newStatus = 2; // 已全部采购
        } else if (anyPurchased) {
            newStatus = 1; // 部分采购
        } else {
            newStatus = 0; // 未采购
        }

        jqf.update(qSalesOrder)
                .set(qSalesOrder.purchaseStatus, newStatus)
                .where(qSalesOrder.id.eq(salesOrderId))
                .execute();
    }

    private String getPurchaseStatusText(Integer purchaseStatus) {
        if (purchaseStatus == null || purchaseStatus == 0) return "待采购";
        if (purchaseStatus == 1) return "部分采购";
        if (purchaseStatus == 2) return "已采购";
        return "待采购";
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSalesOrder.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSalesOrder.accountBookId, accountBookId);
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotBlank(filter)) {
                builder.and(qSalesOrder.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qSalesOrder.orderStatus.eq(state));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qSalesOrder.orderDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qSalesOrder.orderDate.loe(end));
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesOrder.customerId.eq(customerId));
            }
        }

        /** 以销定购采购状态筛选：0待处理/1部分采购/2已采购 */
        public void setPendingStatus(Integer pendingStatus) {
            if (pendingStatus != null) {
                builder.and(qSalesOrder.purchaseStatus.eq(pendingStatus));
            }
        }

        /** 客户名称模糊匹配 */
        public void setCustomerName(String customerName) {
            if (StrUtil.isNotBlank(customerName)) {
                builder.and(qCustomer.name.like("%" + customerName + "%"));
            }
        }

        //查询未出库订单
        public void setQueryUnOutOrder(Integer queryUnOutOrder) {
            if (queryUnOutOrder == 1) {
                // status ("出库单状态 0初始化 1部分出库 2全部出库")
                builder.and(qSalesOrder.status.ne(2));
            }
        }

    }

    /**
     * 以销定购看板 - 查询待采购商品清单
     * 条件：已审核、未关闭、出库状态!=2、待采购数量>0
     */
    public PageResults<PendingPurchaseItemDto> queryPendingPurchase(Page page, Query query) {
        // 查询已审核、未关闭、未全部出库的销售订单
        BooleanBuilder builder = query.builder
                .and(qSalesOrder.orderStatus.eq(OrderStatus.已审核))
                .and(qSalesOrder.closed.isFalse())
                .and(qSalesOrder.status.ne(2));

        long totalSize = bqf.selectFrom(qSalesOrder)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .where(builder).fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder, qCustomer.name)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .where(builder)
                .orderBy(qSalesOrder.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<PendingPurchaseItemDto> result = new ArrayList<>();
        for (Tuple tuple : fetchPage) {
            SalesOrder order = tuple.get(qSalesOrder);
            String customerName = tuple.get(qCustomer.name);

            // 查询该订单的明细
            List<SalesOrderItem> items = bqf.selectFrom(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(order.getId())
                            .and(qSalesOrderItem.merchantId.eq(order.getMerchantId())))
                    .fetch();

            for (SalesOrderItem item : items) {
                BigDecimal orderQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;

                // 计算已出库数量
                BigDecimal[] outAndReturn = calcOutAndReturnQuantity(order.getId(), item.getId());
                BigDecimal outboundQty = outAndReturn[0];
                BigDecimal returnQty = outAndReturn[1];
                BigDecimal pendingOutbound = orderQty.add(returnQty).subtract(outboundQty);
                if (pendingOutbound.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 计算已采购数量
                BigDecimal purchasedQty = calcPurchasedQuantity(order.getId(), item.getProductId(), order.getMerchantId());
                BigDecimal pendingPurchase = pendingOutbound.subtract(purchasedQty);
                if (pendingPurchase.compareTo(BigDecimal.ZERO) <= 0) continue;

                // 查询商品信息
                Product product = bqf.selectFrom(qProduct)
                        .where(qProduct.id.eq(item.getProductId()).and(qProduct.merchantId.eq(order.getMerchantId())))
                        .fetchFirst();

                // 过滤不可采购商品（需求：仅展示商品特性为“可采购”的明细行）
                if (product != null && Boolean.FALSE.equals(product.getPurchasable())) continue;

                // 查询仓库名称
                String warehouseName = "";
                if (item.getWarehouseId() != null) {
                    warehouseName = bqf.selectFrom(QWarehouse.warehouse)
                            .select(QWarehouse.warehouse.name)
                            .where(QWarehouse.warehouse.id.eq(item.getWarehouseId()))
                            .fetchFirst();
                }

                // 查询最近采购价
                BigDecimal lastPrice = getLastPurchasePrice(item.getProductId(), order.getMerchantId());

                // 查询默认供应商
                Long supplierId = null;
                String supplierName = "";
                if (product != null && product.getDefaultSupplierId() != null) {
                    supplierId = product.getDefaultSupplierId();
                    Supplier supplier = bqf.selectFrom(qSupplier)
                            .where(qSupplier.id.eq(supplierId))
                            .fetchFirst();
                    if (supplier != null) supplierName = supplier.getName();
                }

                PendingPurchaseItemDto dto = new PendingPurchaseItemDto();
                dto.setSalesOrderId(order.getId());
                dto.setSalesOrderNo(order.getOrderNo());
                dto.setCustomerName(customerName);
                dto.setOrderDate(order.getOrderDate());
                dto.setProductId(item.getProductId());
                dto.setProductCode(product != null ? product.getCode() : "");
                dto.setProductName(product != null ? product.getName() : "");
                dto.setSpecification(product != null ? product.getSpecification() : "");
                dto.setUnitName(bqf.selectFrom(qUnit).select(qUnit.name)
                        .where(qUnit.id.eq(item.getBaseUnitId())).fetchFirst());
                dto.setBaseUnitId(item.getBaseUnitId());
                dto.setWarehouseId(item.getWarehouseId());
                dto.setWarehouseName(warehouseName);
                dto.setOrderQuantity(orderQty);
                dto.setOutboundQuantity(outboundQty);
                dto.setPendingOutboundQuantity(pendingOutbound);
                dto.setPurchasedQuantity(purchasedQty);
                dto.setPendingPurchaseQuantity(pendingPurchase);
                dto.setSupplierId(supplierId);
                dto.setSupplierName(supplierName);
                dto.setLastPurchasePrice(lastPrice);
                result.add(dto);
            }
        }
        return new PageResults<>(result, page, totalSize);
    }

    /**
     * 以销定购看板 - 查询待采购销售订单（订单级，一行一订单，展开查看商品明细）
     * 条件：已审核、未关闭、出库状态!=2、存在可采购明细、待采购数量>0
     * 状态筛选：0待处理 / 1部分采购 / 2已采购（已采购时展示待采购数量=0 的订单）
     */
    public PageResults<PendingPurchaseOrderDto> queryPendingPurchaseOrders(Page page, Query query, Integer pendingStatus) {
        BooleanBuilder builder = query.builder
                .and(qSalesOrder.orderStatus.eq(OrderStatus.已审核))
                .and(qSalesOrder.closed.isFalse())
                .and(qSalesOrder.status.ne(2));
        if (pendingStatus != null) {
            builder.and(qSalesOrder.purchaseStatus.eq(pendingStatus));
        }
        // 默认（未筛选状态）也展示已采购订单；仅筛选"待处理/部分采购"时排除待采购数量=0 的订单
        boolean includeFullyPurchased = pendingStatus == null || pendingStatus == 2;

        // 先取出全部粗粒度匹配的订单，内存细粒度过滤后再分页，保证"总数=实际展示条数"
        List<Tuple> fetchAll = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder, qCustomer.name)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .where(builder)
                .orderBy(qSalesOrder.id.desc())
                .fetch();

        List<PendingPurchaseOrderDto> result = new ArrayList<>();
        for (Tuple tuple : fetchAll) {
            SalesOrder order = tuple.get(qSalesOrder);
            String customerName = tuple.get(qCustomer.name);

            List<SalesOrderItem> items = bqf.selectFrom(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(order.getId())
                            .and(qSalesOrderItem.merchantId.eq(order.getMerchantId())))
                    .fetch();

            PendingPurchaseOrderDto dto = new PendingPurchaseOrderDto();
            dto.setSalesOrderId(order.getId());
            dto.setSalesOrderNo(order.getOrderNo());
            dto.setCustomerName(customerName);
            dto.setOrderDate(order.getOrderDate());
            dto.setPendingPurchaseQuantity(BigDecimal.ZERO);
            dto.setProductCount(0);

            for (SalesOrderItem item : items) {
                Product product = bqf.selectFrom(qProduct)
                        .where(qProduct.id.eq(item.getProductId()).and(qProduct.merchantId.eq(order.getMerchantId())))
                        .fetchFirst();
                // 过滤不可采购商品
                if (product != null && Boolean.FALSE.equals(product.getPurchasable())) continue;

                BigDecimal orderQty = item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO;
                BigDecimal[] outAndReturn = calcOutAndReturnQuantity(order.getId(), item.getId());
                BigDecimal pendingOutbound = orderQty.add(outAndReturn[1]).subtract(outAndReturn[0]);
                if (pendingOutbound.compareTo(BigDecimal.ZERO) <= 0) continue;

                BigDecimal purchasedQty = calcPurchasedQuantity(order.getId(), item.getProductId(), order.getMerchantId());
                BigDecimal pendingPurchase = pendingOutbound.subtract(purchasedQty);
                if (pendingPurchase.compareTo(BigDecimal.ZERO) < 0) pendingPurchase = BigDecimal.ZERO;
                if (pendingPurchase.compareTo(BigDecimal.ZERO) == 0 && !includeFullyPurchased) continue;

                // 建议供应商（商品默认供应商）
                String supplierName = "";
                Long supplierId = null;
                if (product != null && product.getDefaultSupplierId() != null) {
                    supplierId = product.getDefaultSupplierId();
                    supplierName = bqf.selectFrom(qSupplier).select(qSupplier.name)
                            .where(qSupplier.id.eq(supplierId)).fetchFirst();
                }
                if (StrUtil.isNotBlank(supplierName) && !dto.getSupplierNames().contains(supplierName)) {
                    dto.getSupplierNames().add(supplierName);
                }

                PendingPurchaseItemDto itemDto = new PendingPurchaseItemDto();
                itemDto.setSalesOrderId(order.getId());
                itemDto.setSalesOrderNo(order.getOrderNo());
                itemDto.setCustomerName(customerName);
                itemDto.setOrderDate(order.getOrderDate());
                itemDto.setProductId(item.getProductId());
                itemDto.setProductCode(product != null ? product.getCode() : "");
                itemDto.setProductName(product != null ? product.getName() : "");
                itemDto.setSpecification(product != null ? product.getSpecification() : "");
                itemDto.setUnitName(bqf.selectFrom(qUnit).select(qUnit.name)
                        .where(qUnit.id.eq(item.getBaseUnitId())).fetchFirst());
                itemDto.setBaseUnitId(item.getBaseUnitId());
                itemDto.setWarehouseId(item.getWarehouseId());
                itemDto.setOrderQuantity(orderQty);
                itemDto.setOutboundQuantity(outAndReturn[0]);
                itemDto.setPendingOutboundQuantity(pendingOutbound);
                itemDto.setPurchasedQuantity(purchasedQty);
                itemDto.setPendingPurchaseQuantity(pendingPurchase);
                itemDto.setSupplierId(supplierId);
                itemDto.setSupplierName(supplierName);
                itemDto.setLastPurchasePrice(getLastPurchasePrice(item.getProductId(), order.getMerchantId()));
                dto.getItems().add(itemDto);

                dto.setProductCount(dto.getProductCount() + 1);
                dto.setPendingPurchaseQuantity(dto.getPendingPurchaseQuantity().add(pendingPurchase));
            }

            if (dto.getItems().isEmpty()) continue;

            // 采购状态文本：待处理/部分采购/已采购
            dto.setPurchaseStatusText(calcOrderPurchaseStatusText(dto));
            result.add(dto);
        }

        // 内存分页：总数=细粒度过滤后的条数，与列表实际展示一致
        long totalSize = result.size();
        int fromIndex = Math.min(page.getOffset(), result.size());
        int toIndex = Math.min(page.getOffset() + page.getOffsetEnd(), result.size());
        return new PageResults<>(result.subList(fromIndex, toIndex), page, totalSize);
    }

    /** 计算订单级采购状态文本 */
    private String calcOrderPurchaseStatusText(PendingPurchaseOrderDto dto) {
        if (dto.getPendingPurchaseQuantity() != null && dto.getPendingPurchaseQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            return "已采购";
        }
        boolean anyPurchased = dto.getItems().stream()
                .anyMatch(i -> i.getPurchasedQuantity() != null && i.getPurchasedQuantity().compareTo(BigDecimal.ZERO) > 0);
        return anyPurchased ? "部分采购" : "待处理";
    }

    /** 计算某商品的已采购数量（已审核的采购入库单） */
    private BigDecimal calcPurchasedQuantity(Long salesOrderId, Long productId, Long merchantId) {
        QPurchaseInboundItem qPII = QPurchaseInboundItem.purchaseInboundItem;
        QPurchaseInbound qPI = QPurchaseInbound.purchaseInbound;
        BigDecimal purchased = bqf.selectFrom(qPII)
                .select(qPII.secondaryQuantity.sum())
                .leftJoin(qPI).on(qPI.id.eq(qPII.purchaseInboundId))
                .where(qPI.sourceSalesOrderId.eq(salesOrderId)
                        .and(qPI.orderStatus.eq(OrderStatus.已审核))
                        .and(qPII.productId.eq(productId))
                        .and(qPII.merchantId.eq(merchantId)))
                .fetchFirst();
        return purchased != null ? purchased : BigDecimal.ZERO;
    }

    /** 查询最近采购价（无采购历史时回退到商品档案的预计进货价） */
    private BigDecimal getLastPurchasePrice(Long productId, Long merchantId) {
        QPurchaseInboundItem qPII = QPurchaseInboundItem.purchaseInboundItem;
        QPurchaseInbound qPI = QPurchaseInbound.purchaseInbound;
        BigDecimal price = bqf.selectFrom(qPII)
                .select(qPII.secondaryPrice)
                .leftJoin(qPI).on(qPI.id.eq(qPII.purchaseInboundId))
                .where(qPII.productId.eq(productId)
                        .and(qPII.merchantId.eq(merchantId))
                        .and(qPI.orderStatus.eq(OrderStatus.已审核)))
                .orderBy(qPI.approvedAt.desc())
                .fetchFirst();
        if (price != null && price.compareTo(BigDecimal.ZERO) > 0) {
            return price;
        }
        // 无采购历史或最近采购价为0时，回退到商品档案的预计进货价
        Product product = bqf.selectFrom(qProduct)
                .where(qProduct.id.eq(productId).and(qProduct.merchantId.eq(merchantId)))
                .fetchFirst();
        return product != null && product.getPurchasePrice() != null ? product.getPurchasePrice() : BigDecimal.ZERO;
    }

    /**
     * 批量转采购入库单（按供应商拆分）
     * @return 生成的采购入库单ID列表
     */
    @Transactional
    public List<Long> batchTransferToPurchaseInbound(List<TransferToPurchaseForm> forms,
                                                      Long merchantId, Long adminId, Long accountBookId) {
        List<Long> inboundIds = new ArrayList<>();
        for (TransferToPurchaseForm form : forms) {
            if (form.getSupplierId() == null) {
                throw new ServiceException("请选择供货商");
            }
            if (form.getItems() == null || form.getItems().isEmpty()) {
                continue;
            }
            // 按销售订单分组
            Map<Long, List<PurchaseInboundItem>> itemsByOrder = new LinkedHashMap<>();
            for (PurchaseInboundItem item : form.getItems()) {
                // 这里需要从item中获取salesOrderId，通过form传递
                itemsByOrder.computeIfAbsent(0L, k -> new ArrayList<>()).add(item);
            }
            Long inboundId = transferToPurchaseInbound(
                    form.getSalesOrderId(), form.getSupplierId(), form.getItems(),
                    form.getExpectedDeliveryDate(), form.getRemark(), form.getAutoAudit(), form.getOrderStatus(),
                    merchantId, adminId, accountBookId);
            inboundIds.add(inboundId);
        }
        return inboundIds;
    }

    /**
     * 以销定购 - 生成采购入库单（单订单，按供应商自动拆分）
     * 同一供应商的商品合并到一张采购入库单，不同供应商生成多张
     */
    @Transactional
    public List<Long> createPurchaseInboundBySupplier(CreatePurchaseInboundForm form,
                                                       Long merchantId, Long adminId, Long accountBookId) {
        if (form == null || form.getSaleOrderId() == null) {
            throw new ServiceException("销售订单ID不能为空");
        }
        if (CollectionUtils.isEmpty(form.getItems())) {
            throw new ServiceException("采购明细不能为空");
        }
        // 按供应商分组
        Map<Long, List<CreatePurchaseInboundForm.Item>> group = form.getItems().stream()
                .filter(i -> i.getSupplierId() != null)
                .collect(Collectors.groupingBy(CreatePurchaseInboundForm.Item::getSupplierId, LinkedHashMap::new, Collectors.toList()));
        if (group.isEmpty()) {
            throw new ServiceException("请为商品选择供应商");
        }
        List<TransferToPurchaseForm> forms = new ArrayList<>();
        for (Map.Entry<Long, List<CreatePurchaseInboundForm.Item>> entry : group.entrySet()) {
            TransferToPurchaseForm transferForm = new TransferToPurchaseForm();
            transferForm.setSalesOrderId(form.getSaleOrderId());
            transferForm.setSupplierId(entry.getKey());
            transferForm.setExpectedDeliveryDate(form.getExpectedDeliveryDate());
            transferForm.setRemark(form.getRemark());
            transferForm.setAutoAudit(form.getAutoAudit());
            transferForm.setOrderStatus(form.getOrderStatus());
            List<PurchaseInboundItem> items = new ArrayList<>();
            for (CreatePurchaseInboundForm.Item item : entry.getValue()) {
                PurchaseInboundItem inboundItem = new PurchaseInboundItem();
                inboundItem.setProductId(item.getGoodsId());
                inboundItem.setSecondaryQuantity(item.getQuantity());
                inboundItem.setSecondaryPrice(item.getPurchasePrice());
                inboundItem.setWarehouseId(item.getWarehouseId());
                inboundItem.setBaseUnitId(item.getBaseUnitId());
                inboundItem.setConversionRate(item.getConversionRate());
                items.add(inboundItem);
            }
            transferForm.setItems(items);
            forms.add(transferForm);
        }
        return batchTransferToPurchaseInbound(forms, merchantId, adminId, accountBookId);
    }

    /**
     * 批量设置供应商（更新商品默认供应商）
     */
    @Transactional
    public void batchSetSupplier(List<Long> productIds, Long supplierId, Long merchantId) {
        if (productIds == null || productIds.isEmpty()) {
            throw new ServiceException("请选择商品");
        }
        if (supplierId == null) {
            throw new ServiceException("请选择供应商");
        }
        jqf.update(qProduct)
                .set(qProduct.defaultSupplierId, supplierId)
                .where(qProduct.id.in(productIds).and(qProduct.merchantId.eq(merchantId)))
                .execute();
    }

    /**
     * 订单追踪：查询销售订单的完整采购-入库-出库链
     */
    public OrderTrackingDto trackOrder(Long salesOrderId, Long merchantId) {
        SalesOrder order = bqf.selectFrom(qSalesOrder)
                .where(qSalesOrder.id.eq(salesOrderId).and(qSalesOrder.merchantId.eq(merchantId)))
                .fetchFirst();
        if (order == null) throw new ServiceException("销售订单不存在");

        OrderTrackingDto dto = new OrderTrackingDto();
        dto.setSalesOrderId(order.getId());
        dto.setSalesOrderNo(order.getOrderNo());
        dto.setOrderDate(order.getOrderDate());
        dto.setOrderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : "");
        dto.setPurchaseStatus(order.getPurchaseStatus());
        dto.setPurchaseStatusText(getPurchaseStatusText(order.getPurchaseStatus()));
        dto.setOutboundStatus(order.getStatus());

        // 客户名称
        String customerName = bqf.selectFrom(qCustomer).select(qCustomer.name)
                .where(qCustomer.id.eq(order.getCustomerId())).fetchFirst();
        dto.setCustomerName(customerName);

        // 关联采购入库单
        List<PurchaseInbound> inbounds = bqf.selectFrom(qPurchaseInbound)
                .where(qPurchaseInbound.sourceSalesOrderId.eq(salesOrderId)
                        .and(qPurchaseInbound.merchantId.eq(merchantId)))
                .orderBy(qPurchaseInbound.id.asc())
                .fetch();
        List<OrderTrackingDto.PurchaseInboundBrief> inboundBriefs = new ArrayList<>();
        for (PurchaseInbound pi : inbounds) {
            OrderTrackingDto.PurchaseInboundBrief brief = new OrderTrackingDto.PurchaseInboundBrief();
            brief.setId(pi.getId());
            brief.setOrderNo(pi.getOrderNo());
            brief.setInboundDate(pi.getInboundDate());
            brief.setOrderStatus(pi.getOrderStatus() != null ? pi.getOrderStatus().name() : "");
            brief.setFinalAmount(pi.getFinalAmount());
            brief.setSourceType(pi.getSourceType());
            String supplierName = bqf.selectFrom(qSupplier).select(qSupplier.name)
                    .where(qSupplier.id.eq(pi.getSupplierId())).fetchFirst();
            brief.setSupplierName(supplierName);
            inboundBriefs.add(brief);
        }
        dto.setPurchaseInbounds(inboundBriefs);

        // 关联销售出库单
        List<SalesOutboundItem> outboundItems = bqf.selectFrom(qSalesOutboundItem)
                .where(qSalesOutboundItem.salesOrderId.eq(salesOrderId))
                .fetch();
        Set<Long> outboundIds = outboundItems.stream()
                .map(SalesOutboundItem::getSalesOutboundId)
                .collect(Collectors.toSet());
        List<OrderTrackingDto.SalesOutboundBrief> outboundBriefs = new ArrayList<>();
        if (!outboundIds.isEmpty()) {
            List<SalesOutbound> outbounds = bqf.selectFrom(qSalesOutbound)
                    .where(qSalesOutbound.id.in(outboundIds))
                    .fetch();
            for (SalesOutbound so : outbounds) {
                OrderTrackingDto.SalesOutboundBrief brief = new OrderTrackingDto.SalesOutboundBrief();
                brief.setId(so.getId());
                brief.setOrderNo(so.getOrderNo());
                brief.setOrderStatus(so.getOrderStatus() != null ? so.getOrderStatus().name() : "");
                outboundBriefs.add(brief);
            }
        }
        dto.setSalesOutbounds(outboundBriefs);

        return dto;
    }

    @Transactional
    public void importData(List<SalesOrderImportVo> rows, Long merchantId, Long accountBookId, Long adminId) {
        for (int i = 0; i < rows.size(); i++) {
            SalesOrderImportVo row = rows.get(i);
            int excelRow = i + 2;
            if (StrUtil.isEmpty(row.getCustomerName())) {
                throw new ServiceException("第" + excelRow + "行：客户名称不能为空");
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
        Map<String, List<SalesOrderImportVo>> groups = new LinkedHashMap<>();
        for (SalesOrderImportVo row : rows) {
            String key = StrUtil.isNotEmpty(row.getOrderNo()) ? row.getOrderNo() : "ROW_" + System.nanoTime();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<SalesOrderImportVo>> entry : groups.entrySet()) {
            List<SalesOrderImportVo> group = entry.getValue();
            SalesOrderImportVo first = group.get(0);

            // 查找客户
            Customer customer;
            if (StrUtil.isNotEmpty(first.getCustomerCode())) {
                customer = bqf.selectFrom(qCustomer)
                        .where(qCustomer.code.eq(first.getCustomerCode())
                                .and(qCustomer.merchantId.eq(merchantId))
                                .and(qCustomer.accountBookId.eq(accountBookId)))
                        .fetchFirst();
            } else {
                customer = bqf.selectFrom(qCustomer)
                        .where(qCustomer.name.eq(first.getCustomerName())
                                .and(qCustomer.merchantId.eq(merchantId))
                                .and(qCustomer.accountBookId.eq(accountBookId)))
                        .fetchFirst();
            }
            if (customer == null) {
                throw new ServiceException("客户「" + (StrUtil.isNotEmpty(first.getCustomerCode()) ? first.getCustomerCode() : first.getCustomerName()) + "」不存在");
            }

            // 日期
            LocalDate orderDate;
            try {
                orderDate = LocalDate.parse(first.getOrderDate());
            } catch (Exception e) {
                throw new ServiceException("单据日期格式错误：" + first.getOrderDate());
            }

            // 累计金额
            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalDiscountRate = first.getDiscountRate() != null ? first.getDiscountRate() : BigDecimal.ZERO;

            List<SalesOrderItem> items = new ArrayList<>();
            for (SalesOrderImportVo row : group) {
                Product product = null;
                if (StrUtil.isNotEmpty(row.getProductCode())) {
                    product = bqf.selectFrom(QProduct.product)
                            .where(QProduct.product.code.eq(row.getProductCode()).and(QProduct.product.merchantId.eq(merchantId)))
                            .fetchFirst();
                }
                if (product == null && StrUtil.isNotEmpty(row.getProductName())) {
                    product = bqf.selectFrom(QProduct.product)
                            .where(QProduct.product.name.eq(row.getProductName()).and(QProduct.product.merchantId.eq(merchantId)))
                            .fetchFirst();
                }
                if (product == null) {
                    throw new ServiceException("产品「" + (StrUtil.isNotEmpty(row.getProductCode()) ? row.getProductCode() : row.getProductName()) + "」不存在");
                }
                Warehouse warehouse = null;
                if (StrUtil.isNotEmpty(row.getWarehouseName())) {
                    warehouse = bqf.selectFrom(QWarehouse.warehouse)
                            .where(QWarehouse.warehouse.name.eq(row.getWarehouseName()).and(QWarehouse.warehouse.merchantId.eq(merchantId)))
                            .fetchFirst();
                }
                if (warehouse == null) {
                    warehouse = bqf.selectFrom(QWarehouse.warehouse)
                            .where(QWarehouse.warehouse.merchantId.eq(merchantId).and(QWarehouse.warehouse.systemDefault.isTrue()))
                            .fetchFirst();
                }
                BigDecimal qty = row.getQuantity(); BigDecimal price = row.getUnitPrice();
                BigDecimal dr = row.getDiscountRate() != null ? row.getDiscountRate() : BigDecimal.ZERO;
                BigDecimal st = qty.multiply(price);
                BigDecimal da = st.multiply(dr).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

                SalesOrderItem item = new SalesOrderItem();
                item.setProductId(product.getId()); item.setBaseUnitId(product.getUnitId());
                item.setQuantity(qty); item.setSecondaryQuantity(qty);
                item.setSecondaryUnitId(product.getUnitId()); item.setConversionRate(BigDecimal.ONE);
                item.setUnitPrice(price); item.setDiscountRate(dr); item.setDiscountValue(da);
                item.setSubtotal(st.subtract(da)); item.setWarehouseId(warehouse != null ? warehouse.getId() : null);
                item.setQuantityOut(BigDecimal.ZERO); item.setQuantityReturn(BigDecimal.ZERO);
                item.setCreatedBy(adminId); item.setCreatedAt(LocalDateTime.now());
                item.setMerchantId(merchantId); item.setAccountBookId(accountBookId);
                item.setRemark(row.getRemarks());
                items.add(item);
                totalSubtotal = totalSubtotal.add(st);
            }

            BigDecimal totalDiscountAmount = totalSubtotal.multiply(totalDiscountRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal totalFinalAmount = totalSubtotal.subtract(totalDiscountAmount);

            SalesOrder order = new SalesOrder();
            order.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "销售订单"));
            order.setCustomerId(customer.getId()); order.setOrderDate(orderDate);
            order.setTotalAmount(totalSubtotal); order.setDiscountRate(totalDiscountRate);
            order.setDiscountAmount(totalDiscountAmount); order.setFinalAmount(totalFinalAmount);
            order.setRemarks(first.getRemarks()); order.setOrderStatus(OrderStatus.已保存); order.setStatus(0);
            order.setCreatedBy(adminId); order.setCreatedAt(LocalDateTime.now());
            order.setMerchantId(merchantId); order.setAccountBookId(accountBookId);
            SalesOrder saved = salesOrderRepository.save(order);

            items.forEach(item -> {
                item.setSalesOrderId(saved.getId());
                salesOrderItemRepository.save(item);
            });
        }
    }
}
