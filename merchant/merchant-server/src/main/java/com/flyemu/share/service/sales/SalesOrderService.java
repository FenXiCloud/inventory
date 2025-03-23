package com.flyemu.share.service.sales;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOrderDTO;
import com.flyemu.share.dto.SalesOrderItemDTO;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.repository.*;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @功能描述: 销售订单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesOrderService extends AbsService {

    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final SalesOutboundRepository salesOutboundRepository;

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

    public PageResults<SalesOrderDTO> query(Page page, SalesOrderService.Query query) {
        long totalSize = bqf.selectFrom(qSalesOrder)
                .where(query.builder)
                .fetchCount();

        List<Tuple> fetchPage = bqf.selectFrom(qSalesOrder)
                .select(qSalesOrder, qCustomer.name, qMerchantUser.name,qSalesOutbound.orderNo)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qSalesOrder.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qSalesOrder.createdBy))
                .leftJoin(qSalesOutbound).on(qSalesOutbound.id.eq(qSalesOrder.outOrderId))
                .where(query.builder)
                .orderBy(qSalesOrder.id.desc())
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();

        List<SalesOrderDTO> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            SalesOrderDTO salesOrderDTO = BeanUtil.toBean(tuple.get(qSalesOrder), SalesOrderDTO.class);
            salesOrderDTO.setCustomerName(tuple.get(qCustomer.name));
            salesOrderDTO.setCreatedName(tuple.get(qMerchantUser.name));
            salesOrderDTO.setOutOrderNo(tuple.get(qSalesOutbound.orderNo));
            //查询子表
            List<SalesOrderItem> salesOrderItemList = bqf.selectFrom(qSalesOrderItem)
                    .select(qSalesOrderItem)
                    .where(qSalesOrderItem.salesOrderId.eq(salesOrderDTO.getId()))
                    .fetch();
            List<SalesOrderItemDTO> itemDTOs = new ArrayList<>();
            salesOrderItemList.forEach(item -> {
                SalesOrderItemDTO itemDTO = BeanUtil.toBean(item, SalesOrderItemDTO.class);
                itemDTOs.add(itemDTO);
            });
            salesOrderDTO.setSalesOrderItemList(itemDTOs);

            dtos.add(salesOrderDTO);
        });
        return new PageResults<>(dtos, page, totalSize);
    }

    @Transactional
    public SalesOrder save(SalesOrderForm salesOrderForm) {
        SalesOrder salesOrder = salesOrderForm.getSalesOrder();
        Long id = salesOrder.getId();
        List<SalesOrderItem> salesOrderItemList = salesOrderForm.getSalesOrderItemList();
        if (id != null) {
            //查询
            SalesOrder original = salesOrderRepository.getById(id);
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
                    item.setMerchantId(salesOrder.getMerchantId());
                });
                //批量修改销售订单商品
                salesOrderItemRepository.saveAll(salesOrderItemList);
            }
            return update;
        }else{
            //销售订单状态初始化
            salesOrder.setOrderStatus(OrderStatus.已保存);
            //销售订单编号
            salesOrder.setOrderNo(codeSeedService.generateCode(salesOrder.getMerchantId(), "销售订单"));
            //保存销售订单
            SalesOrder save = salesOrderRepository.save(salesOrder);
            if (!CollectionUtils.isEmpty(salesOrderItemList)) {
                salesOrderItemList.forEach(item -> {
                    //保存价格记录
                    savePrice(item, save);
                    item.setSalesOrderId(save.getId());
                    item.setAccountBookId(salesOrder.getAccountBookId());
                    item.setMerchantId(salesOrder.getMerchantId());
                    item.setCreatedBy(salesOrder.getCreatedBy());
                    item.setCreatedAt(salesOrder.getCreatedAt());
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
        //已关联销售出库单不能删除
        Long outOrderId = original.getOutOrderId();
        if (outOrderId != null){
            Optional<SalesOutbound> salesOutboundOptional = salesOutboundRepository.findById(outOrderId);
            salesOutboundOptional.ifPresent(salesOutbound -> {
                throw new InvalidContextException("已关联销售出库单不能删除");
            });
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

    public SalesOrderDTO getById(SalesOrder query) {
        //查询销售订单
        SalesOrder salesOrder = salesOrderRepository.getById(query.getId());
        //订单数据转换
        SalesOrderDTO dto = BeanUtil.toBean(salesOrder, SalesOrderDTO.class);
        //查询销售订单商品
        List<Tuple> fetch = jqf.selectFrom(qSalesOrderItem)
                .select(qSalesOrderItem, qProduct.code, qProduct.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qSalesOrderItem.productId))
                .leftJoin(qUnit).on(qUnit.id.eq(qSalesOrderItem.baseUnitId))
                .where(qSalesOrderItem.salesOrderId.eq(query.getId())).orderBy(qSalesOrderItem.id.asc()).fetch();
        List<SalesOrderItemDTO> salesOrderItemDTOS = new ArrayList<>();
        fetch.forEach(tuple -> {
            SalesOrderItemDTO salesOrderItemDTO = BeanUtil.toBean(tuple.get(qSalesOrderItem), SalesOrderItemDTO.class);
            salesOrderItemDTO.setProductName(tuple.get(qProduct.name));
            salesOrderItemDTO.setProductCode(tuple.get(qProduct.code));
            salesOrderItemDTO.setUnitName(tuple.get(qUnit.name));
            salesOrderItemDTOS.add(salesOrderItemDTO);
        });
        dto.setSalesOrderItemList(salesOrderItemDTOS);
        return dto;
    }

    @Transactional
    public void batchAudit(SalesOrderForm salesOrderForm) {
        List<Long> orderIds = salesOrderForm.getOrderIds();
        if (orderIds == null || orderIds.isEmpty()) {
            throw new IllegalArgumentException("Order IDs cannot be null or empty");
        }

        List<SalesOrder> salesOrders = salesOrderRepository.findAllById(orderIds);

        if (salesOrders.size() != orderIds.size()) {
            throw new IllegalArgumentException("Some orders could not be found");
        }
        SalesOrder salesOrder = salesOrderForm.getSalesOrder();
        salesOrders.forEach(order -> {
            OrderStatus orderStatus = salesOrderForm.getOrderStatus();
            if (orderStatus.equals(OrderStatus.已保存)) {
                //已关联销售出库单不能审核
                Long outOrderId = order.getOutOrderId();
                if (outOrderId != null){
                    Optional<SalesOutbound> salesOutboundOptional = salesOutboundRepository.findById(outOrderId);
                    salesOutboundOptional.ifPresent(salesOutbound -> {
                        throw new InvalidContextException("已关联销售出库单不能反审核");
                    });
                }
            }
            order.setOrderStatus(orderStatus);
            order.setApprovedAt(LocalDateTime.now());
            order.setApprovedBy(salesOrder.getApprovedBy());
        });

        salesOrderRepository.saveAll(salesOrders);
    }

    @Transactional
    public void audit(SalesOrderForm salesOrderForm) {
        SalesOrder salesOrder = salesOrderForm.getSalesOrder();
        Long id = salesOrder.getId();
        SalesOrder original = salesOrderRepository.getById(id);
        if (original.getId() == null) {
            throw new IllegalArgumentException("单据不存在");
        }
        //反审核
        OrderStatus orderStatus = salesOrder.getOrderStatus();
        if (orderStatus.equals(OrderStatus.已保存)) {
            //已关联销售出库单不能审核
            Long outOrderId = original.getOutOrderId();
            if (outOrderId != null){
                Optional<SalesOutbound> salesOutboundOptional = salesOutboundRepository.findById(outOrderId);
                salesOutboundOptional.ifPresent(salesOutbound -> {
                    throw new InvalidContextException("已关联销售出库单不能反审核");
                });
            }
        }
        original.setApprovedAt(LocalDateTime.now());
        original.setApprovedBy(salesOrder.getApprovedBy());
        original.setOrderStatus(salesOrder.getOrderStatus());
        //审核单据
        salesOrderRepository.save(original);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private String start;
        private String end;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOrder.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOrder.accountBookId.eq(accountBookId));
            }
        }

        public void setFilter(String filter) {
            if (StringUtils.isNotBlank(filter)) {
                builder.and(qSalesOrder.orderNo.like("%" + filter + "%"));
            }
        }

        public void setState(String state) {
            if (StringUtils.isNotBlank(state)) {
                builder.and(qSalesOrder.orderStatus.eq(OrderStatus.valueOf(state)));
            }
        }

        public void setStart(String start) {
            if (StringUtils.isNotBlank(start)) {
                builder.and(qSalesOrder.orderDate.goe(LocalDate.parse(start)));
            }
        }

        public void setEnd(String end) {
            if (StringUtils.isNotBlank(end)) {
                builder.and(qSalesOrder.orderDate.loe(LocalDate.parse(end)));
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
                builder.and(qSalesOrder.outOrderId.isNull());
            }
        }

    }
}
