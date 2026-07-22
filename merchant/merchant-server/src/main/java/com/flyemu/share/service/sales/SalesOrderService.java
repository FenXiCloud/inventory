package com.flyemu.share.service.sales;

import com.flyemu.share.repository.basic.PriceRecordRepository;
import com.flyemu.share.repository.sales.SalesOrderItemRepository;
import com.flyemu.share.repository.sales.SalesOrderRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.common.TenantAware;
import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.sales.SalesOrderDto;
import com.flyemu.share.dto.sales.SalesOrderItemDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
    private final static QInventory qInventory = QInventory.inventory;
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
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;
    private final PriceRecordService priceRecordService;
    private final PriceRecordRepository priceRecordRepository;

    public PageResults<SalesOrderDto> query(Page page, SalesOrderService.Query query) {
        long totalSize = bqf.selectFrom(qSalesOrder)
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
            AtomicReference<Double> totalQuantity = new AtomicReference<>((double) 0L);
            salesOrderItemList.forEach(item -> {
                SalesOrderItemDto itemDTO = BeanUtil.toBean(item, SalesOrderItemDto.class);
                itemDTOs.add(itemDTO);
                Double quantity = itemDTO.getQuantity();
                totalQuantity.updateAndGet(v -> v + quantity);

                //查询销售出库数量和退货数量
                List<Tuple> fetch = bqf.selectFrom(qSalesOutboundItem)
                        .leftJoin(qsalesReturnItem)
                        .on(qsalesReturnItem.salesOutboundId.eq(qSalesOutboundItem.salesOutboundId).and(qsalesReturnItem.outItemId.eq(qSalesOutboundItem.id)))
                        .select(qSalesOutboundItem.quantity, qsalesReturnItem.quantity)
                        .where(qSalesOutboundItem.salesOrderId.eq(item.getSalesOrderId())
                                .and(qSalesOutboundItem.tempId.eq(item.getId())))
                        .fetch();
                //出库数量
                Double outQuantity = fetch.stream()
                        .mapToDouble(tuple1 -> tuple1.get(qSalesOutboundItem.quantity))
                        .sum();
                //退货数量
                Double returnQuantity = fetch.stream()
                        .mapToDouble(tuple2 -> tuple2.get(qsalesReturnItem.quantity) != null ? tuple2.get(qsalesReturnItem.quantity) : 0.0)
                        .sum();

                itemDTO.setQuantityOut(outQuantity);
                itemDTO.setQuantityReturn(returnQuantity);
            });
            salesOrderDTO.setSalesOrderItemList(itemDTOs);
            salesOrderDTO.setTotalQuantity(totalQuantity);
            //关联查询出库单
            subQueryOutOrder(salesOrderDTO);
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

    @Transactional
    public SalesOrder save(SalesOrderForm salesOrderForm, Long merchantId) {
        SalesOrder salesOrder = salesOrderForm.getSalesOrder();
        checkoutService.assertEditable(salesOrder.getMerchantId(), salesOrder.getAccountBookId(), salesOrder.getOrderDate());
        salesOrder.setMerchantId(merchantId);
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
                    //保存价格记录
                    savePrice(item, save);
                    item.setSalesOrderId(save.getId());
                    item.setAccountBookId(salesOrder.getAccountBookId());
                    item.setMerchantId(merchantId);
                    item.setCreatedBy(salesOrder.getCreatedBy());
                    item.setCreatedAt(salesOrder.getCreatedAt());
                    //初始化出库数量
                    item.setQuantityOut(0D);
                    //初始化退货数量
                    item.setQuantityReturn(0D);
                });
                //批量添加销售订单商品
                salesOrderItemRepository.saveAll(salesOrderItemList);
            }
            return save;
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

    @Transactional
    public void delete(Long salesOrderId, Long merchantId, Long accountBookId) {
        SalesOrder original = salesOrderRepository.getById(salesOrderId);
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
        List<Tuple> fetch = jqf.selectFrom(qSalesOrderItem)
                .select(qSalesOrderItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOrderItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOrderItem.baseUnitId))
                .where(qSalesOrderItem.salesOrderId.eq(orderId)
                        .and(qSalesOrderItem.merchantId.eq(merchantId)))
                .orderBy(qSalesOrderItem.id.asc()).fetch();
        List<SalesOrderItemDto> salesOrderItemDTOS = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesOrderItemDto salesOrderItemDTO = BeanUtil.toBean(tuple.get(qSalesOrderItem), SalesOrderItemDto.class);
            salesOrderItemDTO.setProductName(tuple.get(qProduct.name));
            salesOrderItemDTO.setProductCode(tuple.get(qProduct.code));
            salesOrderItemDTO.setUnitName(tuple.get(qUnit.name));
            salesOrderItemDTOS.add(salesOrderItemDTO);
        });
        dto.setSalesOrderItemList(salesOrderItemDTOS);
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
        salesOrders.forEach(order -> {
            if (OrderStatus.已保存.equals(state)) {
                List<SalesOutboundItem> salesOutboundItemList = bqf.selectFrom(qSalesOutboundItem)
                        .where(qSalesOutboundItem.salesOrderId.eq(order.getId()))
                        .fetch();
                if (!CollectionUtils.isEmpty(salesOutboundItemList)) {
                    throw new InvalidContextException("已关联销售出库单不能反审核");
                }
            }
            List<SalesOrderItem> salesOrderItems = bqf.selectFrom(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(order.getId()))
                    .fetch();

            for (SalesOrderItem item : salesOrderItems) {
                Long productId = item.getProductId();
                Long warehouseId = item.getWarehouseId();
                Double quantity = item.getQuantity();

                Inventory inventory = bqf.selectFrom(qInventory)
                        .where(qInventory.productId.eq(productId)
                                .and(qInventory.warehouseId.eq(warehouseId))
                                .and(qInventory.accountBookId.eq(order.getAccountBookId())))
                        .fetchOne();
                Product product = bqf.selectFrom(qProduct)
                        .where(qProduct.id.eq(productId))
                        .fetchOne();
                if (product == null) {
                    throw new InvalidContextException("产品不存在");
                }
                Warehouse warehouse = bqf.selectFrom(qWarehouse)
                        .where(qWarehouse.id.eq(warehouseId))
                        .fetchOne();
                if (warehouse == null) {
                    throw new InvalidContextException("仓库不存在");
                }
                if (inventory == null) {
                    throw new InvalidContextException("仓库中没有该产品的库存: 产品=" + product.getName() + ", 仓库=" + warehouse.getName());
                }
                if (quantity > inventory.getCurrentQuantity()) {
                    throw new InvalidContextException("库存不足: 产品=" + product.getName() + ", 仓库=" + warehouse.getName() +
                            ", 需要数量=" + quantity + ", 当前库存=" + inventory.getCurrentQuantity());
                }
            }

            order.setOrderStatus(state);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(adminId);
        });

        salesOrderRepository.saveAll(salesOrders);
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder.finalAmount.sum())
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                .where(query.builder).fetchFirst();
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

        //查询未出库订单
        public void setQueryUnOutOrder(Integer queryUnOutOrder) {
            if (queryUnOutOrder == 1) {
                // status ("出库单状态 0初始化 1部分出库 2全部出库")
                builder.and(qSalesOrder.status.ne(2));
            }
        }

    }
}
