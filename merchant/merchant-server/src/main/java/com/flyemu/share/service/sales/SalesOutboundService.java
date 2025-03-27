package com.flyemu.share.service.sales;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOutboundDTO;
import com.flyemu.share.dto.SalesOutboundItemDTO;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.repository.*;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.inventory.InventoryService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.flyemu.share.entity.sales.QSalesOrder.salesOrder;

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
public class SalesOutboundService extends AbsService {

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QSalesOutboundItem qSalesOutboundItem = QSalesOutboundItem.salesOutboundItem;

    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;

    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QUnit qUnit = QUnit.unit;

    private final SalesOutboundRepository salesOutboundRepository;
    private final SalesOutboundItemRepository salesOutboundItemRepository;
    private final CodeSeedService codeSeedService;
    private final SalesOrderRepository salesOrderRepository;
    private final SalesReturnRepository salesReturnRepository;

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Autowired
    private InventoryService inventoryService;
    private final PriceRecordService priceRecordService;

    public PageResults<SalesOutboundDTO> query(Page page, SalesOutboundService.Query query) {

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

        List<SalesOutboundDTO> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOutboundDTO salesOutboundDTO = BeanUtil.toBean(tuple.get(qSalesOutbound), SalesOutboundDTO.class);
            salesOutboundDTO.setCustomerName(tuple.get(qCustomer.name));
            salesOutboundDTO.setCreatedName(tuple.get(qMerchantUser.name));

            //查询子表
            List<SalesOutboundItem> salesOutboundItemList = bqf.selectFrom(qSalesOutboundItem)
                    .select(qSalesOutboundItem)
                    .where(qSalesOutboundItem.salesOutboundId.eq(salesOutboundDTO.getId()))
                    .fetch();
            List<SalesOutboundItemDTO> itemDTOs = new ArrayList<>();
            AtomicReference<Double> totalQuantity = new AtomicReference<>((double) 0L);
            salesOutboundItemList.forEach(item -> {
                SalesOutboundItemDTO itemDTO = BeanUtil.toBean(item, SalesOutboundItemDTO.class);
                itemDTOs.add(itemDTO);
                Double quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v + quantity);
            });
            salesOutboundDTO.setSalesOutboundItemList(itemDTOs);
            salesOutboundDTO.setTotalQuantity(totalQuantity);

            //查询关联的销售订单
            List<String> salesOrderList = bqf.selectFrom(qSalesOrder)
                    .select(qSalesOrder.orderNo)
                    .where(qSalesOrder.outOrderId.eq(salesOutboundDTO.getId()))
                    .fetch();
            if (!CollectionUtils.isEmpty(salesOrderList)) {
                salesOutboundDTO.setSalesOrderNos(String.join(",", salesOrderList));
            }

            dtos.add(salesOutboundDTO);
        });

        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public SalesOutbound save(SalesOutboundForm salesOutboundForm) {
        SalesOutbound salesOutbound = salesOutboundForm.getSalesOutbound();
        Long id = salesOutbound.getId();
        List<SalesOutboundItem> salesOutboundItemList = salesOutboundForm.getSalesOutboundItemList();

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
        if (id != null) {
            //查询
            SalesOutbound original = salesOutboundRepository.getById(id);
            //已审核单据不能修改
            OrderStatus orderStatus = original.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已审核)) {
                throw new InvalidContextException("已审核单据不能修改");
            }
            BeanUtil.copyProperties(salesOutbound, original, CopyOptions.create().ignoreNullValue());
            //修改
            SalesOutbound update = salesOutboundRepository.save(original);
            //清除出库单商品
            jqf.delete(qSalesOutboundItem).where(qSalesOutboundItem.salesOutboundId.eq(id)).execute();
            //保存新关系
            if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                salesOutboundItemList.forEach(item -> {
                    //保存价格记录
                    savePrice(item, update);
                    item.setSalesOutboundId(update.getId());
                    item.setAccountBookId(salesOutbound.getAccountBookId());
                    item.setMerchantId(salesOutbound.getMerchantId());
                });
                //批量修改
                salesOutboundItemRepository.saveAll(salesOutboundItemList);
            }
            return update;
        } else {
            //状态初始化
            salesOutbound.setOrderStatus(OrderStatus.已保存);
            //订单编号
            salesOutbound.setOrderNo(codeSeedService.generateCode(salesOutbound.getMerchantId(), "销售出库单"));
            //保存订单
            SalesOutbound save = salesOutboundRepository.save(salesOutbound);
            if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                salesOutboundItemList.forEach(item -> {
                    //保存价格记录
                    savePrice(item, save);
                    item.setSalesOutboundId(save.getId());
                    item.setAccountBookId(salesOutbound.getAccountBookId());
                    item.setMerchantId(salesOutbound.getMerchantId());
                    item.setCreatedBy(salesOutbound.getCreatedBy());
                    item.setCreatedAt(salesOutbound.getCreatedAt());
                });
                //批量保存
                salesOutboundItemRepository.saveAll(salesOutboundItemList);
            }
            //选择的源单不为空
            List<Long> selectSalesOrderIdList = salesOutboundForm.getSelectSalesOrderIdList();
            if (!CollectionUtils.isEmpty(selectSalesOrderIdList)) {
                List<Long> collect = selectSalesOrderIdList.stream().distinct().toList();
                List<SalesOrder> salesOrderList = salesOrderRepository.findAllById(collect);
                salesOrderList.forEach(order -> {
                    //销售订单关联销售出库单
                    order.setOutOrderId(save.getId());
                });
                salesOrderRepository.saveAll(salesOrderList);
            }
            return save;
        }
    }

    private void savePrice(SalesOutboundItem item, SalesOutbound save) {
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

    public Object getById(SalesOrder query) {
        //查询订单
        SalesOutbound salesOutbound = salesOutboundRepository.getById(query.getId());
        //订单数据转换
        SalesOutboundDTO dto = BeanUtil.toBean(salesOutbound, SalesOutboundDTO.class);
        //查询销售订单商品
        List<Tuple> fetch = jqf.selectFrom(qSalesOutboundItem)
                .select(qSalesOutboundItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOutboundItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOutboundItem.baseUnitId))
                .where(qSalesOutboundItem.salesOutboundId.eq(query.getId())).orderBy(qSalesOutboundItem.id.asc()).fetch();
        List<SalesOutboundItemDTO> salesOutboundItemDTOList = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesOutboundItemDTO salesOutboundItemDTO = BeanUtil.toBean(tuple.get(qSalesOutboundItem), SalesOutboundItemDTO.class);
            salesOutboundItemDTO.setProductName(tuple.get(qProduct.name));
            salesOutboundItemDTO.setProductCode(tuple.get(qProduct.code));
            salesOutboundItemDTO.setUnitName(tuple.get(qUnit.name));
            salesOutboundItemDTOList.add(salesOutboundItemDTO);
        });
        dto.setSalesOutboundItemList(salesOutboundItemDTOList);
        return dto;
    }

    @Transactional
    public void batchAudit(SalesOutboundForm salesOutboundForm) {
        List<Long> orderIds = salesOutboundForm.getOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("Order IDs cannot be null or empty");
        }

        List<SalesOutbound> salesOutboundList = salesOutboundRepository.findAllById(orderIds);

        if (salesOutboundList.size() != orderIds.size()) {
            throw new IllegalArgumentException("Some salesOutboundList could not be found");
        }
        SalesOutbound salesOutbound = salesOutboundForm.getSalesOutbound();
        salesOutboundList.forEach(order -> {
            OrderStatus orderStatus = salesOutboundForm.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已保存)) {
                //已关联销售退货单不能审核
                Long returnOrderId = order.getReturnOrderId();
                if (returnOrderId != null){
                    Optional<SalesReturn> salesReturnOptional = salesReturnRepository.findById(returnOrderId);
                    salesReturnOptional.ifPresent(salesReturn -> {
                        throw new InvalidContextException("已关联销售退货单不能审核");
                    });
                }
            }
            order.setOrderStatus(orderStatus);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(salesOutbound.getApprovedBy());
        });

        salesOutboundRepository.saveAll(salesOutboundList);
        // 设置明细
        salesOutboundList.forEach(this::salesOutboundToInventory);
    }

    @Transactional
    public void audit(SalesOutboundForm salesOutboundForm) {
        SalesOutbound salesOutbound = salesOutboundForm.getSalesOutbound();
        Long id = salesOutbound.getId();
        SalesOutbound original = salesOutboundRepository.getById(id);
        if (original.getId() == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        //反审核
        OrderStatus orderStatus = salesOutbound.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已保存)) {
            //已关联销售退货单不能反审核
            Long returnOrderId = original.getReturnOrderId();
            if (returnOrderId != null){
                Optional<SalesReturn> salesReturnOptional = salesReturnRepository.findById(returnOrderId);
                salesReturnOptional.ifPresent(salesReturn -> {
                    throw new InvalidContextException("已关联销售退货单不能反审核");
                });
            }
        }
        original.setApprovedAt(LocalDateTime.now());
        original.setApprovedBy(salesOutbound.getApprovedBy());
        original.setOrderStatus(salesOutbound.getOrderStatus());
        //审核单据
        salesOutboundRepository.save(original);
        // 设置明细
        this.salesOutboundToInventory(original);
    }

    private void salesOutboundToInventory(SalesOutbound original) {
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        List<SalesOutboundItem> outboundItems = jqf.selectFrom(qSalesOutboundItem).where(qSalesOutboundItem.salesOutboundId.eq(original.getId())).fetch();
        //处理库存
        this.getComputedInventory(outboundItems, inventories, inventoryItems, original.getCustomerId(), original.getOrderNo());
        inventories.forEach(item -> {
            if (OrderStatus.已审核.equals(original.getOrderStatus())) {
                // 减库存
                inventoryService.computedInventory(item, false, original.getId(), OperationType.销售出库, inventoryItems);
            } else {
                // 加库存
                inventoryService.computedInventory(item, true, original.getId(), OperationType.销售出库, null);
            }
        });
    }

    private void getComputedInventory(List<SalesOutboundItem> outboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, Long customerId, String orderNo) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        outboundItems.forEach(otherOutboundItem -> {
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
            InventoryItem inventoryItem = getInventoryItem(otherOutboundItem, customerId, orderNo);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    private InventoryItem getInventoryItem(SalesOutboundItem otherOutboundItem, Long customerId, String orderNo) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(otherOutboundItem.getProductId());
        inventoryItem.setWarehouseId(otherOutboundItem.getWarehouseId());
        double parsed = Double.parseDouble(otherOutboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOperationType(OperationType.销售出库);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOrderId(otherOutboundItem.getSalesOutboundId());
        inventoryItem.setMerchantId(otherOutboundItem.getMerchantId());
        inventoryItem.setBatchNumber(orderNo);
        inventoryItem.setAccountBookId(otherOutboundItem.getAccountBookId());
        inventoryItem.setCustomerId(customerId);
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(otherOutboundItem.getCreatedBy());
        inventoryItem.setUnitPrice(otherOutboundItem.getUnitPrice());
        inventoryItem.setSubtotal(otherOutboundItem.getSubtotal());
        return inventoryItem;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOutbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOutbound.accountBookId.eq(accountBookId));
            }
        }

        public void setFilter(String filter) {
            if (StringUtils.isNotBlank(filter)) {
                builder.and(qSalesOutbound.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(String state) {
            if (StringUtils.isNotBlank(state)) {
                builder.and(qSalesOutbound.orderStatus.eq(OrderStatus.valueOf(state)));
            }
        }

        public void setStart(String start) {
            if (StringUtils.isNotBlank(start)) {
                builder.and(qSalesOutbound.outboundDate.goe(LocalDate.parse(start)));
            }
        }

        public void setEnd(String end) {
            if (StringUtils.isNotBlank(end)) {
                builder.and(qSalesOutbound.outboundDate.loe(LocalDate.parse(end)));
            }
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
            }
        }

        //查询未退货订单
        public void setQueryUnReturnOrder(Integer queryUnReturnOrder) {
            if (queryUnReturnOrder == 1) {
                builder.and(qSalesOutbound.returnOrderId.isNull());
            }
        }
    }
}
