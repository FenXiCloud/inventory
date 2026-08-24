package com.flyemu.share.service.purchase;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.PurchaseReportItemDto;
import com.flyemu.share.dto.purchase.PurchaseReportSummaryDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.purchase.*;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseReportService extends BaseService {

    private final static QPurchaseReturn qPurchaseReturn = QPurchaseReturn.purchaseReturn;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QPurchaseReturnItem qPurchaseReturnItem = QPurchaseReturnItem.purchaseReturnItem;
    private final static QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;
    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
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
            if (CollUtil.isNotEmpty(query.supplierIds)) {
                params.put("supplierId", String.join(",", query.supplierIds.stream().map(Object::toString).collect(Collectors.toList())));
            }
            if (query.accountBookId != null) {
                params.put("accountBookId", query.accountBookId);
            }
            if (CollUtil.isNotEmpty(query.warehouseIds)) {
                params.put("warehouseId", String.join(",", query.warehouseIds.stream().map(Object::toString).collect(Collectors.toList())));
            }
            if (CollUtil.isNotEmpty(query.productIds)) {
                params.put("productId", String.join(",", query.productIds.stream().map(Object::toString).collect(Collectors.toList())));
            }
            if (CollUtil.isNotEmpty(query.productCategoryIds)) {
                params.put("productCategoryIds", String.join(",", query.productCategoryIds.stream().map(Object::toString).collect(Collectors.toList())));
            }
            if (CollUtil.isNotEmpty(query.supplierCategoryIds)) {
                params.put("supplierCategoryIds", String.join(",", query.supplierCategoryIds.stream().map(Object::toString).collect(Collectors.toList())));
            }

            org.sagacity.sqltoy.model.Page sqlPage = new org.sagacity.sqltoy.model.Page(page.getSize(), page.getPage());
            org.sagacity.sqltoy.model.Page<PurchaseReportItemDto> summaryPage = lazyDao.findPageBySql(sqlPage, "purchaseReportItemList", params, PurchaseReportItemDto.class);

            return new PageResults<>(summaryPage.getRows(), page, summaryPage.getRecordCount());
        }
    }

    public PageResults<PurchaseReportSummaryDto> summary(Page page, Query query, Set<String> groupValues) {

        Map<String, Object> params = new HashMap<>();
        params.put("startDate", query.start);
        params.put("endDate", query.end);
        params.put("merchantId", query.merchantId);
        if (StrUtil.isNotBlank(query.filter)) {
            params.put("filter", query.filter);
        }
        if (query.accountBookId != null) {
            params.put("accountBookId", query.accountBookId);
        }
        if (CollUtil.isNotEmpty(query.supplierIds)) {
            params.put("supplierIds", String.join(",", query.supplierIds.stream().map(Object::toString).collect(Collectors.toList())));
        }
        if (CollUtil.isNotEmpty(query.warehouseIds)) {
            String warehouseIdsStr = String.join(",", query.warehouseIds.stream().map(Object::toString).collect(Collectors.toList()));
            params.put("warehouseIds", warehouseIdsStr);
        }
        if (CollUtil.isNotEmpty(query.productIds)) {
            String productIdsStr = String.join(",", query.productIds.stream().map(Object::toString).collect(Collectors.toList()));
            params.put("productIds", productIdsStr);
        }
        if (CollUtil.isNotEmpty(query.productCategoryIds)) {
            params.put("productCategoryIds", String.join(",", query.productCategoryIds.stream().map(Object::toString).collect(Collectors.toList())));
        }
        if (CollUtil.isNotEmpty(query.supplierCategoryIds)) {
            params.put("supplierCategoryIds", String.join(",", query.supplierCategoryIds.stream().map(Object::toString).collect(Collectors.toList())));
        }

        String groupBy = "";
        if (groupValues.contains("warehouse")) {
            groupBy += ",po.warehouse_id";
        }

        if (groupValues.contains("supplier")) {
            groupBy += ",po.supplier_id";
        }

        if (groupValues.contains("product")) {
            groupBy += ",po.product_id";
        }

        if (groupBy.isEmpty()) {
            throw new IllegalArgumentException("请选择有效的统计字段");
        }
        groupBy = groupBy.substring(1);

        org.sagacity.sqltoy.model.Page sqlPage = new org.sagacity.sqltoy.model.Page(page.getSize(), page.getPage());
        params.put("groupBy", groupBy);
        org.sagacity.sqltoy.model.Page<PurchaseReportSummaryDto> summaryPage = lazyDao.findPageBySql(sqlPage, "purchaseReportSummaryList", params, PurchaseReportSummaryDto.class);

        return new PageResults<>(summaryPage.getRows(), page, summaryPage.getRecordCount());
    }

    public PageResults<PurchaseReportItemDto> queryReturn(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseReturnItem)
                .select(qPurchaseReturnItem.secondaryPrice, qPurchaseReturnItem.subtotal, qWarehouse.name, qPurchaseReturnItem.secondaryQuantity, qSupplierCategory.name, qSupplier.code,
                        qPurchaseReturn.orderNo, qPurchaseReturn.returnDate, qSupplier.name, qProduct.name, qProduct.code, qUnit.name, qProductCategory.name)
                .leftJoin(qPurchaseReturn).on(qPurchaseReturn.id.eq(qPurchaseReturnItem.purchaseReturnId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseReturn.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseReturnItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseReturnItem.secondaryUnitId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseReturnItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qSupplierCategory).on(qSupplier.supplierCategoryId.eq(qSupplierCategory.id))
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
            dto.setCategoryName(tuple.get(qProductCategory.name));
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setSupplierCode(tuple.get(qSupplier.code));
            dto.setSupplierCategoryName(tuple.get(qSupplierCategory.name));
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setProductName(tuple.get(qProduct.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public PageResults<PurchaseReportItemDto> queryInbound(Page page, Query query) {

        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInboundItem.secondaryPrice, qPurchaseInboundItem.subtotal, qWarehouse.name, qPurchaseInboundItem.secondaryQuantity, qPurchaseInbound.orderNo, qPurchaseInbound.inboundDate, qSupplier.name,
                        qProduct.name, qProduct.code, qUnit.name, qProductCategory.name, qSupplierCategory.name, qSupplier.code)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseInboundItem.secondaryUnitId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qSupplierCategory).on(qSupplier.supplierCategoryId.eq(qSupplierCategory.id))
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
            dto.setCategoryName(tuple.get(qProductCategory.name));
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setSupplierCode(tuple.get(qSupplier.code));
            dto.setSupplierCategoryName(tuple.get(qSupplierCategory.name));
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

    /**
     * 采购统计表：按天/月/仓库/供应商聚合已审核入库明细的采购金额与数量（内存聚合）。
     * query.dimension 传 day / month / warehouse / supplier，缺省按天。
     */
    public List<Map<String, Object>> statistics(Query query) {
        String dimension = StrUtil.blankToDefault(query.dimension, "day");

        List<Tuple> rows = bqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInbound.inboundDate, qSupplier.id, qSupplier.name,
                        qPurchaseInboundItem.warehouseId, qWarehouse.name,
                        qPurchaseInboundItem.quantity, qPurchaseInboundItem.subtotal)
                .leftJoin(qPurchaseInbound).on(qPurchaseInbound.id.eq(qPurchaseInboundItem.purchaseInboundId))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId))
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId))
                .where(query.inboundBuilder.and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核)))
                .fetch();

        Map<String, BigDecimal[]> agg = new TreeMap<>();
        for (Tuple t : rows) {
            LocalDate date = t.get(qPurchaseInbound.inboundDate);
            String name = null;
            if ("warehouse".equalsIgnoreCase(dimension)) {
                name = t.get(qWarehouse.name);
            } else if ("supplier".equalsIgnoreCase(dimension)) {
                name = t.get(qSupplier.name);
            } else if ("month".equalsIgnoreCase(dimension)) {
                name = date == null ? "未登记" : date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
            } else {
                name = date == null ? "未登记" : date.toString();
            }
            if (name == null) {
                name = "未指定";
            }
            BigDecimal[] v = agg.computeIfAbsent(name, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            v[0] = v[0].add(t.get(qPurchaseInboundItem.quantity) == null ? BigDecimal.ZERO : t.get(qPurchaseInboundItem.quantity));
            v[1] = v[1].add(t.get(qPurchaseInboundItem.subtotal) == null ? BigDecimal.ZERO : t.get(qPurchaseInboundItem.subtotal));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, BigDecimal[]> e : agg.entrySet()) {
            BigDecimal[] v = e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("quantity", v[0].setScale(2, RoundingMode.HALF_UP));
            m.put("amount", v[1].setScale(2, RoundingMode.HALF_UP));
            result.add(m);
        }
        return result;
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();
        public final BooleanBuilder inboundBuilder = new BooleanBuilder();

        public String filter;
        public String orderType;
        public Long merchantId;
        public List<Long> warehouseIds;
        public List<Long> supplierIds;
        public List<Long> productIds;
        public List<Long> productCategoryIds;
        public List<Long> supplierCategoryIds;
        public Long accountBookId;
        public LocalDate end;
        public LocalDate start;
        public List<String> groupValues;
        public String dimension;

        public void setGroupValues(List<String> groupValues) {
            if (CollUtil.isEmpty(groupValues)) {
                this.groupValues = groupValues;
                return;
            }
            // 兼容前端 toString() 传参：product,supplier,warehouse
            this.groupValues = groupValues.stream()
                    .filter(StrUtil::isNotBlank)
                    .flatMap(v -> Arrays.stream(v.split(",")))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .distinct()
                    .collect(Collectors.toList());
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

        public void setSupplierIds(List<Long> supplierIds) {
            if (CollUtil.isNotEmpty(supplierIds)) {
                this.supplierIds = supplierIds;
                builder.and(qPurchaseReturn.supplierId.in(supplierIds));
                inboundBuilder.and(qPurchaseInbound.supplierId.in(supplierIds));
            }
        }

        public void setWarehouseIds(List<Long> warehouseIds) {
            if (CollUtil.isNotEmpty(warehouseIds)) {
                this.warehouseIds = warehouseIds;
                builder.and(qPurchaseReturnItem.warehouseId.in(warehouseIds));
                inboundBuilder.and(qPurchaseInboundItem.warehouseId.in(warehouseIds));
            }
        }

        public void setProductIds(List<Long> productIds) {
            if (CollUtil.isNotEmpty(productIds)) {
                this.productIds = productIds;
                builder.and(qPurchaseReturnItem.productId.in(productIds));
                inboundBuilder.and(qPurchaseInboundItem.productId.in(productIds));
            }
        }

        public void setProductCategoryIds(List<Long> productCategoryIds) {
            if (CollUtil.isNotEmpty(productCategoryIds)){
                this.productCategoryIds = productCategoryIds;
                builder.and(qProduct.productCategoryId.in(productCategoryIds));
                inboundBuilder.and(qProduct.productCategoryId.in(productCategoryIds));
            }
        }

        public void setSupplierCategoryIds(List<Long> supplierCategoryIds) {
            if (CollUtil.isNotEmpty(supplierCategoryIds)){
                this.supplierCategoryIds = supplierCategoryIds;
                builder.and(qSupplier.supplierCategoryId.in(supplierCategoryIds));
                inboundBuilder.and(qSupplier.supplierCategoryId.in(supplierCategoryIds));
            }
        }

        public void setOrderType(String orderType) {
            if (StrUtil.isNotEmpty(orderType)) {
                this.orderType = orderType;
            }
        }

        public void setDimension(String dimension) {
            if (StrUtil.isNotEmpty(dimension)) {
                this.dimension = dimension;
            }
        }

    }
}
