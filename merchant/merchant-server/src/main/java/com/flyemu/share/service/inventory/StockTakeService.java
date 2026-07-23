package com.flyemu.share.service.inventory;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.StockTakeDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.StockTakeForm;
import com.flyemu.share.repository.inventory.StockTakeRepository;
import com.flyemu.share.repository.inventory.StockTakeWarehouseRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.ProductCategoryService;
import com.flyemu.share.service.basic.ProductService;
import com.flyemu.share.service.basic.UnitService;
import com.flyemu.share.service.basic.WarehouseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockTakeService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QStockTake qStockTake = QStockTake.stockTake;

    private final StockTakeRepository stockTakeRepository;

    private final StockTakeWarehouseRepository stockTakeWarehouseRepository;

    private final StockTakeItemService stockTakeItemService;

    private final WarehouseService warehouseService;

    private final ProductService productService;

    private final ProductCategoryService productCategoryService;

    private final UnitService unitService;

    private final OtherInboundService otherInboundService;

    private final OtherOutboundService otherOutboundService;

    private final static QAdmin qAdmin = QAdmin.admin;

    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final static QStockTakeItem qStockTakeItem = QStockTakeItem.stockTakeItem;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QUnit qUnit = QUnit.unit;

    private final static QStockTakeWarehouse qStockTakeWarehouse = QStockTakeWarehouse.stockTakeWarehouse;

    public PageResults<StockTakeDto> query(Page page, Query query) {
        PagedList<StockTake> fetchPage = bqf.selectFrom(qStockTake)
                .select(qStockTake)
                .leftJoin(qStockTakeWarehouse).on(qStockTakeWarehouse.stockTakeId.eq(qStockTake.id))
                .leftJoin(qWarehouse).on(qStockTakeWarehouse.warehouseId.eq(qWarehouse.id))
                .leftJoin(qStockTakeItem).on(qStockTakeItem.StockTakeId.eq(qStockTake.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qStockTakeItem.productId))
                .where(query.builder)
                .where(query.builders())
                .groupBy(qStockTake.id)
                .orderBy(qStockTake.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<StockTakeDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            StockTakeDto dto = BeanUtil.toBean(tuple, StockTakeDto.class);
            List<String> fetched = jqf.selectFrom(qStockTakeWarehouse)
                    .select(qWarehouse.name)
                    .leftJoin(qWarehouse).on(qWarehouse.id.eq(qStockTakeWarehouse.warehouseId))
                    .where(qStockTakeWarehouse.stockTakeId.eq(dto.getId())).fetch();
            if (fetched.isEmpty()) {
                dto.setWarehouseName("全部仓库");
            } else {
                StringBuilder warehouseName = new StringBuilder();
                fetched.forEach(name -> {
                    if (StrUtil.isNotEmpty(name)) {
                        warehouseName.append(name).append(",");
                    }
                });
                if (!warehouseName.isEmpty()) {
                    warehouseName.deleteCharAt(warehouseName.length() - 1);
                }
                dto.setWarehouseName(warehouseName.toString());
            }
            Admin admin = jqf.selectFrom(qAdmin).where(qAdmin.id.eq(dto.getCreatedBy())).fetchOne();
            if (admin != null) {
                dto.setCreatedByName(admin.getName());
            }
            // 获取对应关联的其他出库，其他入库订单
            List<String> orderNos = new ArrayList<>();
            List<StockTakeDto.RelatedOrder> relatedOrders = new ArrayList<>();
            List<OtherInbound> otherInbounds = otherInboundService.findByStockTakeId(dto.getId());
            otherInbounds.forEach(inbound -> {
                orderNos.add(inbound.getOrderNo() + "-盘盈单");
                StockTakeDto.RelatedOrder related = new StockTakeDto.RelatedOrder();
                related.setId(inbound.getId());
                related.setOrderNo(inbound.getOrderNo());
                related.setType("inbound");
                related.setLabel(inbound.getOrderNo() + "-盘盈单");
                relatedOrders.add(related);
            });
            List<OtherOutbound> otherOutbounds = otherOutboundService.findByStockTakeId(dto.getId());
            otherOutbounds.forEach(outbound -> {
                orderNos.add(outbound.getOrderNo() + "-盘亏单");
                StockTakeDto.RelatedOrder related = new StockTakeDto.RelatedOrder();
                related.setId(outbound.getId());
                related.setOrderNo(outbound.getOrderNo());
                related.setType("outbound");
                related.setLabel(outbound.getOrderNo() + "-盘亏单");
                relatedOrders.add(related);
            });
            dto.setOrderNos(orderNos);
            dto.setRelatedOrders(relatedOrders);
            dto.setInboundGenerated(otherInbounds != null && !otherInbounds.isEmpty());
            dto.setOutboundGenerated(otherOutbounds != null && !otherOutbounds.isEmpty());
            boolean needInbound = false;
            boolean needOutbound = false;
            List<StockTakeItem> stockTakeItems = stockTakeItemService.findByStockTakeId(dto.getId());
            if (stockTakeItems != null) {
                for (StockTakeItem stockTakeItem : stockTakeItems) {
                    int systemQuantity = Optional.ofNullable(stockTakeItem.getSystemQuantity()).orElse(0);
                    int actualQuantity = Optional.ofNullable(stockTakeItem.getActualQuantity()).orElse(0);
                    int diff = actualQuantity - systemQuantity;
                    if (diff > 0) {
                        needInbound = true;
                    } else if (diff < 0) {
                        needOutbound = true;
                    }
                    if (needInbound && needOutbound) {
                        break;
                    }
                }
            }
            dto.setNeedInbound(needInbound);
            dto.setNeedOutbound(needOutbound);
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public StockTake save(StockTakeForm stockTakeForm, Long merchantId) {
        StockTake result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        StockTake stockTake = stockTakeForm.getStockTake();
        java.time.LocalDate __checkoutOrderDate = stockTake.getCheckDate() == null ? null : new java.sql.Date(stockTake.getCheckDate().getTime()).toLocalDate();
        checkoutService.assertEditable(stockTake.getMerchantId(), stockTake.getAccountBookId(), __checkoutOrderDate);
        stockTake.setMerchantId(merchantId);
        if (stockTake.getId() != null) {
            //更新
            StockTake original = stockTakeRepository.getById(stockTake.getId());
            BeanUtil.copyProperties(stockTake, original, CopyOptions.create().ignoreNullValue());
            result = stockTakeRepository.save(original);
        } else {
            stockTake.setCreatedAt(LocalDateTime.now());
            stockTake.setOrderNo(snowflakeGenerator.next().toString());
            stockTake.setOrderStatus(OrderStatus.已保存);
            result = stockTakeRepository.save(stockTake);
        }
        // 处理仓库关联
        this.doStockTakeWarehouse(stockTake);
        //处理盘点单明细
        stockTakeItemService.generateStockTakeDetails(result, stockTakeForm.getStockTakeItems());
        return result;
    }

    /**
     * 处理仓库关联
     *
     * @param stockTake 盘点单
     */
    private void doStockTakeWarehouse(StockTake stockTake) {
        //处理历史仓库关联
        jqf.delete(qStockTakeWarehouse).where(qStockTakeWarehouse.stockTakeId.eq(stockTake.getId())).execute();
        //新增仓库关联
        String warehouseIds = stockTake.getWarehouseIds();
        List<StockTakeWarehouse> insertWarehouses = new ArrayList<>();
        if (StrUtil.isNotBlank(warehouseIds)) {
            Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).forEach(warehouseId -> {
                StockTakeWarehouse stockTakeWarehouse = new StockTakeWarehouse();
                stockTakeWarehouse.setWarehouseId(warehouseId);
                stockTakeWarehouse.setStockTakeId(stockTake.getId());
                stockTakeWarehouse.setAccountBookId(stockTake.getAccountBookId());
                stockTakeWarehouse.setMerchantId(stockTake.getMerchantId());
                stockTakeWarehouse.setCreatedBy(stockTake.getCreatedBy());
                stockTakeWarehouse.setCreatedAt(stockTake.getCreatedAt());
                insertWarehouses.add(stockTakeWarehouse);
            });
        }
        if (!insertWarehouses.isEmpty()) {
            stockTakeWarehouseRepository.saveAll(insertWarehouses);
        }
    }

    @Transactional
    public void delete(Long stockTakeId, Long merchantId, Long accountBookId) {
        if (Boolean.TRUE.equals(this.existOrder(stockTakeId))) {
            throw new ServiceException("已生成对应盘点单据～");
        }
        jqf.delete(qStockTakeItem).where(qStockTakeItem.StockTakeId.eq(stockTakeId).and(qStockTakeItem.merchantId.eq(merchantId)).and(qStockTakeItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qStockTake)
                .where(qStockTake.id.eq(stockTakeId).and(qStockTake.merchantId.eq(merchantId)).and(qStockTake.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<StockTake> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qStockTake).where(qStockTake.merchantId.eq(merchantId).and(qStockTake.accountBookId.eq(accountBookId))).fetch();
    }

    public List<Map<String, Object>> load(Long merchantId, Long id) {
        StringTemplate dateExpressions = Expressions.
                stringTemplate("DATE_FORMAT({0},'%Y-%m-%d')", qStockTake.checkDate);
        List<Tuple> fetch = jqf.selectFrom(qStockTake)
                .select(
                        qStockTake.id.as("id"),
                        dateExpressions.as("checkDate"),
                        qStockTake.remarks.as("remarks"),
                        qStockTake.orderStatus.as("orderStatus"),
                        qStockTake.warehouseId.as("mainWarehouseId"),
                        qStockTake.warehouseIds.as("mainWarehouseIds"),
                        qStockTakeItem.id.as("itemId"),
                        qStockTakeItem.actualQuantity.as("actualQuantity"),
                        qStockTakeItem.systemQuantity.as("systemQuantity"),
                        qStockTakeItem.warehouseId.as("warehouseId"),
                        qWarehouse.name.as("warehouseName"),
                        qProduct.id.as("productId"),
                        qProduct.name.as("productName"),
                        qProduct.code.as("productCode"),
                        qProduct.imgPath.as("productImgPath"),
                        qProduct.specification.as("productSpecification"),
                        qProductCategory.name.as("productCategoryName"),
                        qUnit.id.as("baseUnitId"),
                        qUnit.name.as("productUnitName"),
                        qAdmin.name.as("adminName"),
                        qStockTakeItem.differenceReason.as("differenceReason")
                )
                .leftJoin(qStockTakeItem).on(qStockTakeItem.StockTakeId.eq(qStockTake.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qStockTakeItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qStockTakeItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qStockTake.createdBy))
                .where(qStockTake.id.eq(id).and(qStockTake.merchantId.eq(merchantId)))
                .fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> item;
        for (Tuple tuple : fetch) {
            item = new HashMap<>();
            Integer actualQuantity = tuple.get(qStockTakeItem.actualQuantity.as("actualQuantity"));
            Integer systemQuantity = tuple.get(qStockTakeItem.systemQuantity.as("systemQuantity"));
            item.put("id", tuple.get(qStockTake.id.as("id")));
            item.put("checkDate", tuple.get(dateExpressions.as("checkDate")));
            item.put("remarks", tuple.get(qStockTake.remarks.as("remarks")));
            item.put("orderStatus", tuple.get(qStockTake.orderStatus.as("orderStatus")));
            item.put("mainWarehouseId", tuple.get(qStockTake.warehouseId.as("mainWarehouseId")));
            item.put("mainWarehouseIds", tuple.get(qStockTake.warehouseIds.as("mainWarehouseIds")));
            item.put("itemId", tuple.get(qStockTakeItem.id.as("itemId")));
            item.put("actualQuantity", actualQuantity);
            item.put("systemQuantity", systemQuantity);
            item.put("warehouseId", tuple.get(qStockTakeItem.warehouseId.as("warehouseId")));
            item.put("warehouseName", tuple.get(qWarehouse.name.as("warehouseName")));
            item.put("productId", tuple.get(qProduct.id.as("productId")));
            item.put("productName", tuple.get(qProduct.name.as("productName")));
            item.put("productCode", tuple.get(qProduct.code.as("productCode")));
            item.put("productImgPath", tuple.get(qProduct.imgPath.as("productImgPath")));
            item.put("productSpecification", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productCategoryName", tuple.get(qProductCategory.name.as("productCategoryName")));
            item.put("baseUnitId", tuple.get(qUnit.id.as("baseUnitId")));
            item.put("productUnitName", tuple.get(qUnit.name.as("productUnitName")));
            item.put("adminName", tuple.get(qAdmin.name.as("adminName")));
            if (actualQuantity != null && systemQuantity != null) {
                item.put("deficient", actualQuantity - systemQuantity);
            } else {
                item.put("deficient", 0);
            }
            item.put("differenceReason", tuple.get(qStockTakeItem.differenceReason.as("differenceReason")));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择单据");
        }
        if (OrderStatus.已保存.equals(state)) {
            if (Boolean.TRUE.equals(this.existOrders(ids))) {
                throw new ServiceException("审核数据中有已生成盘点单据的数据～");
            }
        } else if (!OrderStatus.已审核.equals(state)) {
            throw new ServiceException("不支持的审核状态");
        }
        for (Long id : ids) {
            this.approve(id, state, adminId, merchantId);
        }
    }

    private void approve(Long id, OrderStatus state, Long adminId, Long merchantId) {
        StockTake stockTake = jqf.selectFrom(qStockTake)
                .where(qStockTake.id.eq(id).and(qStockTake.merchantId.eq(merchantId)))
                .fetchOne();
        if (stockTake == null) {
            throw new ServiceException("审核数据不存在～");
        }
        if (OrderStatus.已审核.equals(state)) {
            stockTake.setOrderStatus(OrderStatus.已审核);
            stockTake.setApprovedBy(adminId);
            stockTake.setApprovedAt(LocalDateTime.now());
            // 调整对应库存
            stockTakeRepository.save(stockTake);
        } else if (OrderStatus.已保存.equals(state)) {
            stockTake.setOrderStatus(OrderStatus.已保存);
            stockTake.setApprovedBy(adminId);
            stockTake.setApprovedAt(LocalDateTime.now());
            // 调整对应库存
            stockTakeRepository.save(stockTake);
        }
    }

    public Map<String, Object> export(Long id) {
        Map<String, Object> result = new HashMap<>(8);
        StockTake stockTake = jqf.selectFrom(qStockTake).where(qStockTake.id.eq(id).and(qStockTake.orderStatus.eq(OrderStatus.已审核))).fetchOne();
        if (stockTake == null) {
            return result;
        }
        List<StockTakeItem> stockTakeItems = stockTakeItemService.findByStockTakeId(id);
        List<Map<String, Object>> inbounds = this.getInbounds(stockTakeItems);
        List<Map<String, Object>> outbounds = this.getOutbounds(stockTakeItems);
        List<OtherInbound> otherInbounds = otherInboundService.findByStockTakeId(id);
        List<OtherOutbound> otherOutbounds = otherOutboundService.findByStockTakeId(id);
        boolean inboundGenerated = otherInbounds != null && !otherInbounds.isEmpty();
        boolean outboundGenerated = otherOutbounds != null && !otherOutbounds.isEmpty();
        result.put("needInbound", !inbounds.isEmpty());
        result.put("needOutbound", !outbounds.isEmpty());
        result.put("inboundGenerated", inboundGenerated);
        result.put("outboundGenerated", outboundGenerated);
        // 仅返回尚未生成的待开单据明细
        result.put("inbounds", inboundGenerated ? Collections.emptyList() : inbounds);
        result.put("outbounds", outboundGenerated ? Collections.emptyList() : outbounds);
        return result;
    }

    /**
     * 获取盘点盘盈
     *
     * @param stockTakeItems 盘点明细列表
     * @return 盘亏列表
     */
    private List<Map<String, Object>> getOutbounds(List<StockTakeItem> stockTakeItems) {
        List<Map<String, Object>> outbounds = new ArrayList<>();
        for (StockTakeItem stockTakeItem : stockTakeItems) {
            Integer systemQuantity = Optional.ofNullable(stockTakeItem.getSystemQuantity()).orElse(0);
            Integer actualQuantity = Optional.ofNullable(stockTakeItem.getActualQuantity()).orElse(0);
            int diff = actualQuantity - systemQuantity;
            if (diff < 0) {
                Map<String, Object> item = new HashMap<>();
                this.getStockBoundsItem(stockTakeItem, item, systemQuantity - actualQuantity);
                outbounds.add(item);
            }
        }
        return outbounds;
    }

    /**
     * 组装盘盈/盘亏明细（供其他出入库单据带入）
     */
    private void getStockBoundsItem(StockTakeItem stockTakeItem, Map<String, Object> item, Integer quantity) {
        Long productId = stockTakeItem.getProductId();
        Long warehouseId = stockTakeItem.getWarehouseId();
        Product product = productService.loadById(productId, stockTakeItem.getMerchantId());
        ProductCategory productCategory = productCategoryService.loadById(product.getMerchantId(), product.getProductCategoryId());
        Unit unit = unitService.selectByPrimaryKey(product.getUnitId());
        Warehouse warehouse = warehouseService.selectByPrimaryKey(warehouseId);
        java.math.BigDecimal unitPrice = product.getPurchasePrice() != null
                ? product.getPurchasePrice()
                : java.math.BigDecimal.ZERO;
        java.math.BigDecimal qty = java.math.BigDecimal.valueOf(quantity == null ? 0 : quantity);
        item.put("productName", product.getName());
        item.put("productId", product.getId());
        item.put("productCode", product.getCode());
        item.put("productSpecification", product.getSpecification());
        item.put("productCategoryId", product.getProductCategoryId());
        item.put("productCategoryName", productCategory != null ? productCategory.getName() : null);
        item.put("productUnitName", unit != null ? unit.getName() : null);
        item.put("productUnitId", product.getUnitId());
        item.put("warehouseName", warehouse != null ? warehouse.getName() : null);
        item.put("warehouseId", warehouse != null ? warehouse.getId() : warehouseId);
        item.put("quantity", quantity);
        item.put("unitPrice", unitPrice);
        item.put("subtotal", unitPrice.multiply(qty));
    }

    private List<Map<String, Object>> getInbounds(List<StockTakeItem> stockTakeItems) {
        List<Map<String, Object>> inbounds = new ArrayList<>();
        for (StockTakeItem stockTakeItem : stockTakeItems) {
            Integer systemQuantity = Optional.ofNullable(stockTakeItem.getSystemQuantity()).orElse(0);
            Integer actualQuantity = Optional.ofNullable(stockTakeItem.getActualQuantity()).orElse(0);
            int diff = actualQuantity - systemQuantity;
            if (diff > 0) {
                Map<String, Object> item = new HashMap<>();
                this.getStockBoundsItem(stockTakeItem, item, actualQuantity - systemQuantity);
                inbounds.add(item);
            }
        }
        return inbounds;
    }

    /**
     * 判断是否有对应盘点单据
     *
     * @param stockTakeId 盘点单id
     * @return true/false
     */
    public Boolean existOrder(Long stockTakeId) {
        List<OtherInbound> otherInbounds = otherInboundService.findByStockTakeId(stockTakeId);
        List<OtherOutbound> otherOutbounds = otherOutboundService.findByStockTakeId(stockTakeId);
        return (otherOutbounds != null && !otherOutbounds.isEmpty()) ||
                (otherInbounds != null && !otherInbounds.isEmpty());
    }

    public Boolean existOrders(List<Long> idList) {
        Boolean result = false;
        for (Long id : idList) {
            result = this.existOrder(id);
            if (result) {
                break;
            }
        }
        return result;
    }

    public Boolean existOrders(String ids) {
        List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).toList();
        return this.existOrders(idList);
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private OrderStatus state;

        private Long warehouseId;

        private String filter;

        private String warehouseIds;

        private String productCategoryIds;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qStockTake.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qStockTake.accountBookId, accountBookId);
        }

        private static Date addTimeOfFinalMoment(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.HOUR, 23);
            calendar.add(Calendar.MINUTE, 59);
            calendar.add(Calendar.SECOND, 59);
            return calendar.getTime();
        }

        public BooleanBuilder builders() {
            if (start != null && end != null) {
                builder.and(qStockTake.checkDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qStockTake.checkDate.goe(start));
            }
            if (state != null) {
                builder.and(qStockTake.orderStatus.eq(state));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qStockTake.orderNo.contains(filter))
                        .or(qWarehouse.name.contains(filter));
            }
            if (warehouseId != null) {
                builder.and(qStockTake.warehouseId.eq(warehouseId))
                        .or(qStockTake.warehouseId.isNull());
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                builder.and(qStockTakeWarehouse.warehouseId.in(Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList()).or(qStockTakeWarehouse.id.isNull()));
            }
            if (StrUtil.isNotBlank(productCategoryIds)) {
                builder.and(qProduct.productCategoryId.in(Arrays.stream(productCategoryIds.split(",")).map(Long::parseLong).toList()));
            }
            return builder;
        }
    }
}
