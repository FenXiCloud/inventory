package com.flyemu.share.service.purchase;

import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.PurchaseReportItemDto;
import com.flyemu.share.dto.purchase.PurchaseReportSummaryDto;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * @功能描述: 采购订单报表
 * @创建时间: 2025年02月23日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseReportService extends AbsService {

    private final static QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QPurchaseReturnItem qPurchaseReturnItem = QPurchaseReturnItem.purchaseReturnItem;
    private final static QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;

    public PageResults<PurchaseReportItemDto> query(Page page, Query query) {
        if ("in".equals(query.orderType)) {
            return queryInbound(page, query);

        } else if ("out".equals(query.orderType)) {
            return queryReturn(page, query);
        } else {
            Map<String, Object> params = new HashMap<>();
            params.put("startDate", query.start);
            params.put("endDate", query.end);
            params.put("merchantId", query.merchantId);
            if (StrUtil.isNotBlank(query.filter)) {
                params.put("filter", query.filter);
            }
            if (query.supplierId != null) {
                params.put("supplierId", query.supplierId);
            }
            if (query.accountBookId != null) {
                params.put("accountBookId", query.accountBookId);
            }
            if (query.warehouseId != null) {
                params.put("warehouseId", query.warehouseId);
            }
            if (query.productId != null) {
                params.put("productId", query.productId);
            }

            org.sagacity.sqltoy.model.Page sqlPage = new org.sagacity.sqltoy.model.Page(page.getSize(), page.getPage());
            org.sagacity.sqltoy.model.Page<PurchaseReportItemDto> summaryPage = lazyDao.findPageBySql(sqlPage, "purchaseReportItemList", params, PurchaseReportItemDto.class);

            return new PageResults<>(summaryPage.getRows(), page, summaryPage.getRecordCount());
        }
    }

    public PageResults<PurchaseReportSummaryDto> queryStat(Page page, Query query, Set<String> groupValues) {

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", query.start);
        params.put("endDate", query.end);
        params.put("merchantId", query.merchantId);
        if (StrUtil.isNotBlank(query.filter)) {
            params.put("filter", query.filter);
        }
        if (query.supplierId != null) {
            params.put("supplierId", query.supplierId);
        }
        if (query.accountBookId != null) {
            params.put("accountBookId", query.accountBookId);
        }
        if (query.warehouseId != null) {
            params.put("warehouseId", query.warehouseId);
        }
        if (query.productId != null) {
            params.put("productId", query.productId);
        }

        String groupBy = "po.product_id";
        if (groupValues.contains("warehouse")) {
            groupBy += ",po.warehouse_id";
        }

        org.sagacity.sqltoy.model.Page sqlPage = new org.sagacity.sqltoy.model.Page(page.getSize(), page.getPage());

        if ("in".equals(query.orderType)) {
            if (groupValues.contains("supplier")) {
                groupBy += ",p2_0.supplier_id";
            }
            params.put("groupBy", groupBy);
            org.sagacity.sqltoy.model.Page<PurchaseReportSummaryDto> summaryPage = lazyDao.findPageBySql(sqlPage, "purchaseOrderReportSummaryList", params, PurchaseReportSummaryDto.class);

            return new PageResults<>(summaryPage.getRows(), page, summaryPage.getRecordCount());
        } else if ("out".equals(query.orderType)) {
            if (groupValues.contains("supplier")) {
                groupBy += ",p2_0.supplier_id";
            }
            params.put("groupBy", groupBy);
            org.sagacity.sqltoy.model.Page<PurchaseReportSummaryDto> summaryPage = lazyDao.findPageBySql(sqlPage, "purchaseReturnReportSummaryList", params, PurchaseReportSummaryDto.class);

            return new PageResults<>(summaryPage.getRows(), page, summaryPage.getRecordCount());
        } else {

            if (groupValues.contains("supplier")) {
                groupBy += ",po.supplier_id";
            }
            params.put("groupBy", groupBy);
            org.sagacity.sqltoy.model.Page<PurchaseReportSummaryDto> summaryPage = lazyDao.findPageBySql(sqlPage, "purchaseReportSummaryList", params, PurchaseReportSummaryDto.class);

            return new PageResults<>(summaryPage.getRows(), page, summaryPage.getRecordCount());
        }
    }

    public PageResults<PurchaseReportItemDto> queryReturn(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseReturnItem)
                .select(qPurchaseReturnItem.secondaryPrice, qPurchaseReturnItem.subtotal, qWarehouse.name, qPurchaseReturnItem.secondaryQuantity,
                        qPurchaseReturn.orderNo, qPurchaseReturn.returnDate, qSupplier.name, qProduct.name, qProduct.code, qUnit.name)
                .leftJoin(qPurchaseReturn).on(qPurchaseReturn.id.eq(qPurchaseReturnItem.purchaseReturnId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReturn.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseReturnItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReturnItem.secondaryUnitId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReturnItem.productId))
                .where(query.builder.and(qPurchaseReturn.orderStatus.eq(OrderStatus.已审核)))
                .orderBy(qPurchaseReturnItem.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseReportItemDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReportItemDto dto = new PurchaseReportItemDto();
            dto.setSecondaryUnitName(tuple.get(qUnit.name));
            dto.setSecondaryQuantity(tuple.get(qPurchaseReturnItem.secondaryQuantity));
            dto.setSecondaryPrice(tuple.get(qPurchaseReturnItem.secondaryPrice));
            dto.setSubtotal(tuple.get(qPurchaseReturnItem.subtotal));
            dto.setOrderNo(tuple.get(qPurchaseReturn.orderNo));
            dto.setOrderDate(tuple.get(qPurchaseReturn.returnDate));
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dto.setOrderType("采购退货");
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public PageResults<PurchaseReportItemDto> queryInbound(Page page, Query query) {

        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInboundItem.secondaryPrice, qPurchaseInboundItem.subtotal, qWarehouse.name, qPurchaseInboundItem.secondaryQuantity, qPurchaseInbound.orderNo, qPurchaseInbound.inboundDate, qSupplier.name, qProduct.name, qProduct.code, qUnit.name)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseInboundItem.secondaryUnitId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId))
                .where(query.inboundBuilder.and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核))).orderBy(qPurchaseInboundItem.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseReportItemDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReportItemDto dto = new PurchaseReportItemDto();
            dto.setSecondaryUnitName(tuple.get(qUnit.name));
            dto.setSecondaryQuantity(tuple.get(qPurchaseInboundItem.secondaryQuantity));
            dto.setSecondaryPrice(tuple.get(qPurchaseInboundItem.secondaryPrice));
            dto.setSubtotal(tuple.get(qPurchaseInboundItem.subtotal));
            dto.setOrderNo(tuple.get(qPurchaseInbound.orderNo));
            dto.setOrderDate(tuple.get(qPurchaseInbound.inboundDate));
            dto.setOrderType("采购入库");
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public PageResults<PurchaseReportSummaryDto> queryInboundStat(Page page, Query query, Set<String> groupValues) {
        List<Path> groupByFields = new ArrayList<>();
        List<Expression<?>> expressions = new ArrayList<>();
        expressions.add(qPurchaseInboundItem.quantity.sum());
        expressions.add(qPurchaseInboundItem.subtotal.sum());
        expressions.add(qProduct.name);
        expressions.add(qProduct.code);
        expressions.add(qProduct.id);
        expressions.add(qUnit.name);

        groupByFields.add(qPurchaseInboundItem.productId);
        if (groupValues.contains("supplier")) {
            groupByFields.add(qPurchaseInbound.supplierId);
            expressions.add(qSupplier.name);
        }
        if (groupValues.contains("warehouse")) {
            groupByFields.add(qPurchaseInboundItem.warehouseId);
            expressions.add(qWarehouse.name);
        }

        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInboundItem)
                .select(expressions.toArray(new Expression[0]))
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseInboundItem.baseUnitId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId))
                .where(query.inboundBuilder.and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(groupByFields.toArray(new Path[0]))
                .orderBy(qPurchaseInboundItem.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseReportSummaryDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReportSummaryDto dto = new PurchaseReportSummaryDto();
            dto.setProductId(tuple.get(qProduct.id));
            dto.setBaseUnitName(tuple.get(qUnit.name));
            dto.setBaseQuantitySum(tuple.get(qPurchaseInboundItem.quantity.sum()));
            dto.setSubtotalSum(tuple.get(qPurchaseInboundItem.subtotal.sum()));
            dto.setOrderType("采购入库");
            if (tuple.get(qWarehouse.name) != null) {
                dto.setWarehouseName(tuple.get(qWarehouse.name));
            }
            if (tuple.get(qSupplier.name) != null) {
                dto.setSupplierName(tuple.get(qSupplier.name));
            }
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }


    public PageResults<PurchaseReportSummaryDto> queryReturnStat(Page page, Query query, Set<String> groupValues) {
        List<Path> groupByFields = new ArrayList<>();
        groupByFields.add(qPurchaseReturnItem.productId);


        if (groupValues.contains("supplier")) {
            groupByFields.add(qPurchaseReturn.supplierId);
        }
        if (groupValues.contains("warehouse")) {
            groupByFields.add(qPurchaseReturnItem.warehouseId);
        }

        Path[] array = groupByFields.toArray(new Path[0]);
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseReturnItem)
                .select(qSupplier.name, qWarehouse.name, qPurchaseReturnItem.quantity.sum(), qPurchaseReturnItem.subtotal.sum(), qProduct.id, qProduct.accountBookId, qProduct.name, qProduct.code, qUnit.name)
                .leftJoin(qPurchaseReturn).on(qPurchaseReturn.id.eq(qPurchaseReturnItem.purchaseReturnId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReturn.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseReturnItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReturnItem.secondaryUnitId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReturnItem.productId))
                .groupBy(qPurchaseReturnItem.productId)
                .where(query.builder.and(qPurchaseReturn.orderStatus.eq(OrderStatus.已审核)))
                .orderBy(qPurchaseReturnItem.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseReportSummaryDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseReportSummaryDto dto = new PurchaseReportSummaryDto();
            dto.setProductId(tuple.get(qProduct.id));
            dto.setBaseUnitName(tuple.get(qUnit.name));
            dto.setBaseQuantitySum(tuple.get(qPurchaseReturnItem.quantity.sum()));
            dto.setSubtotalSum(tuple.get(qPurchaseReturnItem.subtotal.sum()));
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dto.setOrderType("采购退货");
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();
        public final BooleanBuilder inboundBuilder = new BooleanBuilder();

        public String filter;
        public String orderType;
        public Long merchantId;
        public Long warehouseId;
        public Long supplierId;
        public Long productId;
        public Long accountBookId;
        public LocalDate end;
        public LocalDate start;
        public List<String> groupValues;

        public void setGroupValues(List<String> groupValues) {
            this.groupValues = groupValues;
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                this.merchantId = merchantId;
                builder.and(qPurchaseReturn.merchantId.eq(merchantId));
                inboundBuilder.and(qPurchaseInbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                this.accountBookId = accountBookId;
                builder.and(qPurchaseReturn.accountBookId.eq(accountBookId));
                inboundBuilder.and(qPurchaseInbound.accountBookId.eq(accountBookId));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                this.start = start;
                builder.and(qPurchaseReturn.returnDate.goe(start));
                inboundBuilder.and(qPurchaseInbound.inboundDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                this.end = end;
                builder.and(qPurchaseReturn.returnDate.loe(end));
                inboundBuilder.and(qPurchaseInbound.inboundDate.loe(end));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                this.filter = filter;
                builder.and(qPurchaseReturn.orderNo.contains(filter).or(qSupplier.name.contains(filter)));
                inboundBuilder.and(qPurchaseInbound.orderNo.contains(filter).or(qSupplier.name.contains(filter)));
            }
        }

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                this.supplierId = supplierId;
                builder.and(qPurchaseReturn.supplierId.eq(supplierId));
                inboundBuilder.and(qPurchaseInbound.supplierId.eq(supplierId));
            }
        }

        public void setWarehouseId(Long warehouseId) {
            if (warehouseId != null) {
                this.warehouseId = warehouseId;
                builder.and(qPurchaseReturnItem.warehouseId.eq(warehouseId));
                inboundBuilder.and(qPurchaseInboundItem.warehouseId.eq(warehouseId));
            }
        }

        public void setProductId(Long productId) {
            if (productId != null) {
                this.productId = productId;
                builder.and(qPurchaseReturnItem.productId.eq(productId));
                inboundBuilder.and(qPurchaseInboundItem.productId.eq(productId));
            }
        }

        public void setOrderType(String orderType) {
            if (StrUtil.isNotEmpty(orderType)) {
                this.orderType = orderType;
            }
        }

    }
}
