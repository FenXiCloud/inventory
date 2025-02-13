package com.flyemu.share.service.sales;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.SalesOrderDTO;
import com.flyemu.share.dto.SalesOrderItemDTO;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.sales.*;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.repository.SalesOrderItemRepository;
import com.flyemu.share.repository.SalesOrderRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 销售报表
 * @作者: wl
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SalesReportService extends AbsService {

    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;
    private final static QSalesOrderItem qSalesOrderItem = QSalesOrderItem.salesOrderItem;

    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;

    private final SalesOrderRepository salesOrderRepository;
    private final SalesOrderItemRepository salesOrderItemRepository;
    private final CodeSeedService codeSeedService;
    private final static QCustomer qCustomer = QCustomer.customer;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;

    public PageResults<SalesOrderDTO> salesItem(Page page, SalesReportService.Query query) {
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
