package com.flyemu.share.service.sales;

import com.flyemu.share.repository.basic.ProductRepository;
import com.flyemu.share.repository.basic.WarehouseRepository;
import com.flyemu.share.repository.sales.SalesOrderItemRepository;
import com.flyemu.share.repository.sales.SalesOrderRepository;
import com.flyemu.share.repository.sales.SalesOutboundItemRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.repository.sales.SalesReturnRepository;
import com.flyemu.share.common.TenantAware;
import com.alibaba.fastjson.JSONObject;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.json.JSONUtil;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOutboundImportVo;
import com.flyemu.share.dto.sales.SalesOutboundDto;
import com.flyemu.share.dto.sales.SalesOutboundItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.service.fund.SettlementService;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private final SettlementService settlementService;

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
            AtomicReference<BigDecimal> totalQuantity = new AtomicReference<>(BigDecimal.ZERO);
            salesOutboundItemList.forEach(item -> {
                SalesOutboundItemDto itemDTO = BeanUtil.toBean(item, SalesOutboundItemDto.class);
                itemDTOs.add(itemDTO);
                BigDecimal quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v.add(quantity));
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
            // 结算状态
            QSettlement qS = QSettlement.settlement;
            QSettlementItem qSI = QSettlementItem.settlementItem;
            String status = jqf.select(qS.orderStatus.stringValue())
                    .from(qSI)
                    .innerJoin(qS).on(qS.id.eq(qSI.settlementId))
                    .where(qSI.businessId.eq(salesOutboundDTO.getId())
                            .and(qSI.businessCategory.eq("INVENTORY"))
                            .and(qSI.businessType.eq("销售出库单")))
                    .fetchFirst();
            salesOutboundDTO.setSettlementStatus(status);
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
            //清除旧商品
            jqf.delete(qSalesOutboundItem)
                    .where(qSalesOutboundItem.salesOutboundId.eq(id))
                    .execute();
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
            BigDecimal quantity = salesOrderItemDB.getQuantity();
            //数据库中的退货数量
            BigDecimal quantityReturn = salesOrderItemDB.getQuantityReturn();
            //页面传递过来的出库数量
            BigDecimal quantityOut = salesOutboundItem.getQuantity();
            SalesOrder order = salesOrderRepository.getById(salesOrderId);
            if (quantityOut.compareTo(quantity.add(quantityReturn)) < 0) {
                //部分出库 第一此更新，后续有兜底逻辑
                order.setStatus(1);
                salesOrderRepository.save(order);
            }
            //出库数量 出库数量是累加的
            salesOrderItemDB.setQuantityOut(quantityOut.add(salesOrderItemDB.getQuantityOut()));
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
            if (salesOrderItemList.stream().allMatch(item -> (item.getQuantityOut().compareTo(item.getQuantity().add(item.getQuantityReturn())) >= 0))) {
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
            BigDecimal totalQuantity = salesOutboundItemListDB.stream()
                    .map(item -> item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            log.info("销售出库单商品数量：{}", totalQuantity);
            if (totalQuantity.compareTo(BigDecimal.ZERO) > 0) {
                salesOrderUpdate.setStatus(1);
            }

            //统计所有销售订单商品数量
            List<SalesOrderItem> salesOrderItemListDB = bqf.selectFrom(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(salesOrderId))
                    .fetch();
            BigDecimal totalQuantityOrder = salesOrderItemListDB.stream()
                    .map(item -> item.getQuantity() != null ? item.getQuantity() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            log.info("销售订单商品数量：{}", totalQuantityOrder);
            if (totalQuantity.compareTo(totalQuantityOrder) >= 0) {
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

        // 结账日期校验：已结账的单据不能删除
        checkoutService.assertEditable(original.getMerchantId(), original.getAccountBookId(), original.getOutboundDate());

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
                .where(qSalesOutboundItem.salesOutboundId.eq(salesOutboundId)
                        .and(qSalesOutboundItem.salesOrderId.isNotNull()))
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
    //销售出库单详情查询接口：根据商户 ID + 出库单 ID，查询出库单头部信息、出库明细，并且补齐商品名称、编码、单位名称；同时如果明细关联销售订单，顺带查出销售订单编号，组装成 DTO 返回前端详情页渲染。
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
    //销售出库单开票预填：返回购买方名称/税号 + 开票明细（商品名/数量/单价/税率），供开票页自动带出
    public Map<String, Object> prefillInvoice(Long merchantId, Long outboundId) {
        SalesOutboundDto dto = load(merchantId, outboundId);
        Customer customer = dto.getCustomerId() != null ? customerService.findById(dto.getCustomerId()) : null;

        List<Map<String, Object>> items = new ArrayList<>();
        if (dto.getSalesOutboundItemList() != null) {
            for (SalesOutboundItemDto it : dto.getSalesOutboundItemList()) {
                Map<String, Object> m = new HashMap<>();
                m.put("goodsName", it.getProductName() != null ? it.getProductName() : "");
                m.put("quantity", it.getQuantity());
                m.put("unitPrice", it.getUnitPrice());
                m.put("taxRate", it.getTaxRate() != null ? it.getTaxRate() : new BigDecimal("0.06"));
                items.add(m);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("buyerName", customer != null ? customer.getName() : "");
        result.put("buyerTaxNo", customer != null ? customer.getTaxNo() : "");
        result.put("items", items);
        return result;
    }
    //销售出库单审核
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

        // 结账日期校验：已结账的单据不能审核/反审核
        for (SalesOutbound order : salesOutboundList) {
            checkoutService.assertEditable(order.getMerchantId(), order.getAccountBookId(), order.getOutboundDate());
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
                // 删除关联的结算单
                jqf.delete(QSettlementItem.settlementItem)
                        .where(QSettlementItem.settlementItem.businessId.eq(order.getId())
                                .and(QSettlementItem.settlementItem.businessCategory.eq("INVENTORY"))
                                .and(QSettlementItem.settlementItem.businessType.eq("销售出库单")))
                        .execute();
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
        // 审核时自动生成结算单
        if (OrderStatus.已审核.equals(state)) {
            salesOutboundList.forEach(order -> {
                String customerName = bqf.selectFrom(qCustomer).select(qCustomer.name)
                        .where(qCustomer.id.eq(order.getCustomerId())).fetchOne();
                settlementService.createFromOrder(order.getMerchantId(), order.getAccountBookId(),
                        1, order.getCustomerId(), customerName != null ? customerName : "",
                        order.getOrderNo(), order.getFinalAmount(), order.getId(), "销售出库单");
            });
        }
    }
    //看是否有付款单或核销单
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
    //修改客户应收余额和生成一条往来的流水记录
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
    //构造一条客户往来流水实体
    private CustomerFlow getCustomerFlow(SalesOutbound salesOutbound, OrderStatus targetStatus) {
        CustomerFlow flow = new CustomerFlow();
        flow.setCustomerId(salesOutbound.getCustomerId());
        flow.setBusinessId(salesOutbound.getId());
        flow.setBusinessNo(salesOutbound.getOrderNo());
        flow.setBusinessDate(salesOutbound.getOutboundDate());

        BigDecimal finalAmount = salesOutbound.getFinalAmount();

        flow.setRemarks(targetStatus == OrderStatus.已审核 ? "销售出库单审核通过" : "销售出库单反审核");
        //添加流水
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
    //在销售出库单审核和反审核时，更新库存余额和批次
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
    //销售出库审核时，调用成本服务扣减成本批次、算出本行商品出库成本；然后把成本单价、成本金额回填到出库明细并保存入库
    private void applyIssueCost(SalesOutbound original, List<SalesOutboundItem> outboundItems) {
        for (SalesOutboundItem line : outboundItems) {
            int qty = line.getQuantity() == null ? 0 : line.getQuantity().intValue();
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
    //反审核时，清空成本
    private void clearIssueCost(List<SalesOutboundItem> outboundItems) {
        for (SalesOutboundItem line : outboundItems) {
            line.setCostPrice(null);
            line.setCostAmount(null);
        }
        salesOutboundItemRepository.saveAll(outboundItems);
    }
    //销售出库场景下，在内存里完成库存数量、库存总成本的变动计算，同时生成库存变动流水对象
    private void getComputedInventory(List<SalesOutboundItem> outboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, SalesOutbound salesOutbound) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        outboundItems.forEach(outboundItem -> {
            BigDecimal quantity = outboundItem.getQuantity();
            Long productId = outboundItem.getProductId();
            // 优先使用审核写入的成本；反审时回补同样金额
            BigDecimal subtotal = outboundItem.getCostAmount();
            if (subtotal == null) {
                Inventory inv = inventoryService.findByWarehouseIdAndProductId(
                        outboundItem.getWarehouseId(), productId);
                BigDecimal avg = inv != null && inv.getAverageCost() != null ? inv.getAverageCost() : BigDecimal.ZERO;
                subtotal = avg.multiply(quantity == null ? BigDecimal.ZERO : quantity)
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            BigDecimal finalSubtotal = subtotal;
            // 库存数量、库存总成本
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
                                int parsed = quantity.intValue();
                                currentQuantity += parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setProductId(outboundItem.getProductId());
                                inventory.setWarehouseId(outboundItem.getWarehouseId());
                                int parsed = outboundItem.getQuantity().intValue();
                                inventory.setCurrentQuantity(parsed);
                                inventory.setTotalCost(finalSubtotal);
                                inventory.setMerchantId(outboundItem.getMerchantId());
                                inventory.setBaseUnitId(outboundItem.getBaseUnitId());
                                inventory.setAccountBookId(outboundItem.getAccountBookId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            // 库存变动流水
            InventoryItem inventoryItem = getInventoryItem(outboundItem, salesOutbound, finalSubtotal);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }
    //组装一条库存变动流水记录
    private InventoryItem getInventoryItem(SalesOutboundItem outboundItem, SalesOutbound salesOutbound,
                                           BigDecimal costAmount) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(outboundItem.getProductId());
        inventoryItem.setWarehouseId(outboundItem.getWarehouseId());
        int parsed = outboundItem.getQuantity().intValue();
        inventoryItem.setQuantity(parsed);
        inventoryItem.setBaseUnitId(outboundItem.getBaseUnitId());
        inventoryItem.setOperationType(OperationType.销售出库);
        inventoryItem.setOrderId(outboundItem.getSalesOutboundId());
        inventoryItem.setMerchantId(outboundItem.getMerchantId());
        inventoryItem.setBatchNumber(outboundItem.getBatchNumber() != null && !outboundItem.getBatchNumber().isBlank()
                ? outboundItem.getBatchNumber() : salesOutbound.getOrderNo());
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
//    统计销售出库汇总数据：符合条件的出库单总金额 + 所有出库明细总数量，返回给前端做统计看板、报表。
    public Map<String, BigDecimal> queryTotal(Query query) {
        // 得到销售出库总金额
        BigDecimal amount = bqf.selectFrom(qSalesOutbound)
                .select(qSalesOutbound.finalAmount.sum())
                .where(query.builder).fetchFirst();
        // 得到筛选范围内销售出库总数量
        BigDecimal quantity = bqf.selectFrom(qSalesOutboundItem)
                .select(qSalesOutboundItem.quantity.sum())
                .where(qSalesOutboundItem.salesOutboundId.in(
                        bqf.selectFrom(qSalesOutbound).select(qSalesOutbound.id).where(query.builder)
                )).fetchFirst();
        Map<String, BigDecimal> result = new HashMap<>();
        result.put("amount", amount);
        result.put("quantity", java.util.Objects.requireNonNullElse(quantity, BigDecimal.ZERO));
        return result;
    }

    // 导出销售出库单（表头级，一单一列）
    public List<JSONObject> exportList(Query query) {
        BooleanBuilder builder = query != null ? query.builder : new BooleanBuilder();
        List<Tuple> tuples = bqf.selectFrom(qSalesOutbound)
                .select(qSalesOutbound, qCustomer.name, qMerchantUser.name)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOutbound.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOutbound.createdBy))
                .where(builder)
                .orderBy(qSalesOutbound.id.desc())
                .fetch();

        List<Long> ids = tuples.stream().map(t -> t.get(qSalesOutbound).getId()).collect(Collectors.toList());
        Map<Long, BigDecimal> qtyMap = new HashMap<>();
        if (!ids.isEmpty()) {
            bqf.selectFrom(qSalesOutboundItem)
                    .select(qSalesOutboundItem.salesOutboundId, qSalesOutboundItem.quantity)
                    .where(qSalesOutboundItem.salesOutboundId.in(ids))
                    .fetch()
                    .forEach(t -> {
                        Long oid = t.get(qSalesOutboundItem.salesOutboundId);
                        BigDecimal qty = t.get(qSalesOutboundItem.quantity);
                        qtyMap.merge(oid, qty != null ? qty : BigDecimal.ZERO, BigDecimal::add);
                    });
        }

        List<JSONObject> list = new ArrayList<>();
        for (Tuple tuple : tuples) {
            SalesOutbound o = tuple.get(qSalesOutbound);
            JSONObject json = new JSONObject();
            json.put("出库日期", o.getOutboundDate() != null ? o.getOutboundDate().toString() : "");
            json.put("订单编号", o.getOrderNo() != null ? o.getOrderNo() : "");
            json.put("客户", tuple.get(qCustomer.name) != null ? tuple.get(qCustomer.name) : "");
            json.put("销售金额", o.getTotalAmount() != null ? o.getTotalAmount().toString() : "");
            json.put("折扣金额", o.getDiscountAmount() != null ? o.getDiscountAmount().toString() : "");
            json.put("折后金额", o.getFinalAmount() != null ? o.getFinalAmount().toString() : "");
            json.put("数量", qtyMap.getOrDefault(o.getId(), BigDecimal.ZERO).toString());
            json.put("制单人", tuple.get(qMerchantUser.name) != null ? tuple.get(qMerchantUser.name) : "");
            json.put("状态", o.getOrderStatus() != null ? o.getOrderStatus().name() : "");
            json.put("备注", o.getRemarks() != null ? o.getRemarks() : "");
            list.add(json);
        }
        return list;
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

    @Transactional
    public void importData(List<SalesOutboundImportVo> rows, Long merchantId, Long accountBookId, Long adminId) {
        for (int i = 0; i < rows.size(); i++) {
            SalesOutboundImportVo row = rows.get(i);
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
        Map<String, List<SalesOutboundImportVo>> groups = new LinkedHashMap<>();
        for (SalesOutboundImportVo row : rows) {
            String key = StrUtil.isNotEmpty(row.getOrderNo()) ? row.getOrderNo() : "ROW_" + System.nanoTime();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(row);
        }

        for (Map.Entry<String, List<SalesOutboundImportVo>> entry : groups.entrySet()) {
            List<SalesOutboundImportVo> group = entry.getValue();
            SalesOutboundImportVo first = group.get(0);

            Customer customer;
            if (StrUtil.isNotEmpty(first.getCustomerCode())) {
                customer = bqf.selectFrom(qCustomer).where(qCustomer.code.eq(first.getCustomerCode()).and(qCustomer.merchantId.eq(merchantId)).and(qCustomer.accountBookId.eq(accountBookId))).fetchFirst();
            } else {
                customer = bqf.selectFrom(qCustomer).where(qCustomer.name.eq(first.getCustomerName()).and(qCustomer.merchantId.eq(merchantId)).and(qCustomer.accountBookId.eq(accountBookId))).fetchFirst();
            }
            if (customer == null) throw new ServiceException("客户「" + (StrUtil.isNotEmpty(first.getCustomerCode()) ? first.getCustomerCode() : first.getCustomerName()) + "」不存在");

            LocalDate outboundDate;
            try { outboundDate = LocalDate.parse(first.getOutboundDate()); } catch (Exception e) { throw new ServiceException("出库日期格式错误：" + first.getOutboundDate()); }

            BigDecimal totalSubtotal = BigDecimal.ZERO;
            BigDecimal totalDiscountRate = first.getDiscountRate() != null ? first.getDiscountRate() : BigDecimal.ZERO;
            List<SalesOutboundItem> items = new ArrayList<>();

            for (SalesOutboundImportVo row : group) {
                Product product = null;
                if (StrUtil.isNotEmpty(row.getProductCode())) {
                    product = bqf.selectFrom(QProduct.product).where(QProduct.product.code.eq(row.getProductCode()).and(QProduct.product.merchantId.eq(merchantId))).fetchFirst();
                }
                if (product == null && StrUtil.isNotEmpty(row.getProductName())) {
                    product = bqf.selectFrom(QProduct.product).where(QProduct.product.name.eq(row.getProductName()).and(QProduct.product.merchantId.eq(merchantId))).fetchFirst();
                }
                if (product == null) throw new ServiceException("产品「" + (StrUtil.isNotEmpty(row.getProductCode()) ? row.getProductCode() : row.getProductName()) + "」不存在");

                Warehouse warehouse = null;
                if (StrUtil.isNotEmpty(row.getWarehouseName())) {
                    warehouse = bqf.selectFrom(QWarehouse.warehouse).where(QWarehouse.warehouse.name.eq(row.getWarehouseName()).and(QWarehouse.warehouse.merchantId.eq(merchantId))).fetchFirst();
                }
                if (warehouse == null) warehouse = bqf.selectFrom(QWarehouse.warehouse).where(QWarehouse.warehouse.merchantId.eq(merchantId).and(QWarehouse.warehouse.systemDefault.isTrue())).fetchFirst();

                BigDecimal qty = row.getQuantity(); BigDecimal price = row.getUnitPrice();
                BigDecimal dr = row.getDiscountRate() != null ? row.getDiscountRate() : BigDecimal.ZERO;
                BigDecimal st = qty.multiply(price);
                BigDecimal da = st.multiply(dr).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);

                SalesOutboundItem item = new SalesOutboundItem();
                item.setProductId(product.getId()); item.setBaseUnitId(product.getUnitId());
                item.setQuantity(qty); item.setSecondaryQuantity(qty);
                item.setSecondaryUnitId(product.getUnitId()); item.setConversionRate(BigDecimal.ONE);
                item.setUnitPrice(price); item.setDiscountRate(dr); item.setDiscountValue(da);
                item.setSubtotal(st.subtract(da)); item.setWarehouseId(warehouse != null ? warehouse.getId() : null);
                item.setCreatedBy(adminId); item.setCreatedAt(LocalDateTime.now());
                item.setMerchantId(merchantId); item.setAccountBookId(accountBookId);
                item.setRemark(row.getRemarks()); items.add(item);
                totalSubtotal = totalSubtotal.add(st);
            }

            BigDecimal totalDiscountAmount = totalSubtotal.multiply(totalDiscountRate).divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            SalesOutbound outbound = new SalesOutbound();
            outbound.setOrderNo(codeSeedService.generateCode(merchantId, accountBookId, "销售出库单"));
            outbound.setCustomerId(customer.getId()); outbound.setOutboundDate(outboundDate);
            outbound.setTotalAmount(totalSubtotal); outbound.setDiscountRate(totalDiscountRate);
            outbound.setDiscountAmount(totalDiscountAmount); outbound.setFinalAmount(totalSubtotal.subtract(totalDiscountAmount));
            outbound.setRemarks(first.getRemarks()); outbound.setOrderStatus(OrderStatus.已保存);
            outbound.setCreatedBy(adminId); outbound.setCreatedAt(LocalDateTime.now());
            outbound.setMerchantId(merchantId); outbound.setAccountBookId(accountBookId);
            outbound.setVerifiedAmount(BigDecimal.ZERO); outbound.setCollectionAmount(BigDecimal.ZERO);
            SalesOutbound saved = salesOutboundRepository.save(outbound);

            for (SalesOutboundItem item : items) {
                item.setSalesOutboundId(saved.getId());
                salesOutboundItemRepository.save(item);
            }
        }
    }
}
