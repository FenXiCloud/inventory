package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.StockTakeDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.StockTakeForm;
import com.flyemu.share.repository.StockTakeRepository;
import com.flyemu.share.service.AbsService;
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

/**
 * @功能描述: 盘点单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StockTakeService extends AbsService {

    private final static QStockTake qStockTake = QStockTake.stockTake;

    private final StockTakeRepository stockTakeRepository;

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

    public PageResults<StockTakeDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qStockTake)
                .select(qStockTake, qWarehouse.name, qAdmin.name)
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qStockTake.warehouseId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qStockTake.createdBy))
                .where(query.builder)
                .where(query.builders())
                .orderBy(qStockTake.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<StockTakeDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            StockTakeDto dto = BeanUtil.toBean(tuple.get(qStockTake), StockTakeDto.class);
            String warehouseName = tuple.get(qWarehouse.name);
            if (StrUtil.isBlank(warehouseName)) {
                dto.setWarehouseName("全部仓库");
            } else {
                dto.setWarehouseName(warehouseName);
            }
            dto.setCreatedByName(tuple.get(qAdmin.name));
            // 获取对应关联的其他出库，其他入库订单
            List<String> orderNos = new ArrayList<>();
            List<OtherInbound> otherInbounds = otherInboundService.findByStockTakeId(dto.getId());
            otherInbounds.forEach(inbound -> {
                orderNos.add(inbound.getOrderNo());
            });
            List<OtherOutbound> otherOutbounds = otherOutboundService.findByStockTakeId(dto.getId());
            otherOutbounds.forEach(outbound -> {
                orderNos.add(outbound.getOrderNo());
            });
            dto.setOrderNos(orderNos);
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public StockTake save(StockTakeForm stockTakeForm) {
        StockTake result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        StockTake stockTake = stockTakeForm.getStockTake();
        if (stockTake.getId() != null) {
            //更新
            StockTake original = stockTakeRepository.getById(stockTake.getId());
            BeanUtil.copyProperties(stockTake, original, CopyOptions.create().ignoreNullValue());
            result = stockTakeRepository.save(original);
        } else {
            stockTake.setCreatedAt(LocalDateTime.now());
            stockTake.setOrderNo(snowflakeGenerator.next().toString());
            result = stockTakeRepository.save(stockTake);
        }
        //处理盘点单明细
        stockTakeItemService.generateStockTakeDetails(result, stockTakeForm.getStockTakeItems());
        return result;
    }

    @Transactional
    public void delete(Long stockTakeId, Long merchantId, Long accountBookId) {
        jqf.delete(qStockTakeItem).where(qStockTakeItem.StockTakeId.eq(stockTakeId).and(qStockTakeItem.merchantId.eq(merchantId)).and(qStockTakeItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qStockTake)
                .where(qStockTake.id.eq(stockTakeId).and(qStockTake.merchantId.eq(merchantId)).and(qStockTake.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<StockTake> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qStockTake).where(qStockTake.merchantId.eq(merchantId).and(qStockTake.accountBookId.eq(accountBookId))).fetch();
    }

    public List<Map<String, Object>> load(Long id) {
        StringTemplate dateExpressions = Expressions.
                stringTemplate("DATE_FORMAT({0},'%Y-%m-%d')", qStockTake.checkDate);
        List<Tuple> fetch = jqf.selectFrom(qStockTake)
                .select(
                        qStockTake.id.as("id"),
                        dateExpressions.as("checkDate"),
                        qStockTake.remarks.as("remarks"),
                        qStockTake.warehouseId.as("mainWarehouseId"),
                        qStockTakeItem.id.as("itemId"),
                        qStockTakeItem.actualQuantity.as("actualQuantity"),
                        qStockTakeItem.systemQuantity.as("systemQuantity"),
                        qStockTakeItem.warehouseId.as("warehouseId"),
                        qWarehouse.name.as("warehouseName"),
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
                .where(qStockTake.id.eq(id))
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
            item.put("mainWarehouseId", tuple.get(qStockTake.warehouseId.as("warehouseId")));
            item.put("itemId", tuple.get(qStockTakeItem.id.as("itemId")));
            item.put("actualQuantity", actualQuantity);
            item.put("systemQuantity", systemQuantity);
            item.put("warehouseId", tuple.get(qStockTakeItem.warehouseId.as("warehouseId")));
            item.put("warehouseName", tuple.get(qWarehouse.name.as("warehouseName")));
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
    public void approve(Long id, ApproveType type, Long adminId) {
        StockTake stockTake = jqf.selectFrom(qStockTake).where(qStockTake.id.eq(id)).fetchOne();
        switch (type) {
            case AUDITS -> {
                stockTake.setOrderStatus(OrderStatus.已审核);
                stockTake.setApprovedBy(adminId);
                stockTake.setApprovedAt(LocalDateTime.now());
                // 调整对应库存
                stockTakeRepository.save(stockTake);
            }
            case ANTI_AUDIT -> {
                jqf.delete(qStockTake).where(qStockTake.id.eq(id)).execute();
                stockTakeItemService.deleteByStockTakeId(id);
            }
            default -> {

            }
        }
    }

    public Map<String, Object> export(Long id) {
        Map<String, Object> result = new HashMap<>(2);
        List<StockTakeItem> stockTakeItems = stockTakeItemService.findByStockTakeId(id);
        // 是否已有关联盘盈数据
        List<OtherInbound> otherInbounds = otherInboundService.findByStockTakeId(id);
        if (otherInbounds == null || otherInbounds.isEmpty()) {
            // 获取盘点盘盈
            List<Map<String, Object>> inbounds = this.getInbounds(stockTakeItems);
            // 设置盘盈数据
            result.put("inbounds", inbounds);
        }
        // 是否已有关联盘亏数据
        List<OtherOutbound> otherOutbounds = otherOutboundService.findByStockTakeId(id);
        if (otherOutbounds == null || otherOutbounds.isEmpty()) {
            // 获取盘点盘亏
            List<Map<String, Object>> outbounds = this.getOutbounds(stockTakeItems);
            // 设置盘亏数据
            result.put("outbounds", outbounds);
        }
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
        Map<String, Object> item;
        for (StockTakeItem stockTakeItem : stockTakeItems) {
            Integer systemQuantity = stockTakeItem.getSystemQuantity();
            Integer actualQuantity = stockTakeItem.getActualQuantity();
            if (actualQuantity - systemQuantity > 0) {
                item = new HashMap<>();
                // 盘盈
                this.getStockBoundsItem(stockTakeItem, item, actualQuantity - systemQuantity);
                outbounds.add(item);
            }
        }
        return outbounds;
    }

    /**
     * 获取盘点单数据
     *
     * @param stockTakeItem 盘点单对象
     * @param item          设置对象
     * @param quantity      数量
     */
    private void getStockBoundsItem(StockTakeItem stockTakeItem, Map<String, Object> item, Integer quantity) {
        Long productId = stockTakeItem.getProductId();
        Long warehouseId = stockTakeItem.getWarehouseId();
        Product product = productService.loadById(productId, stockTakeItem.getMerchantId());
        ProductCategory productCategory = productCategoryService.loadById(product.getMerchantId(), product.getProductCategoryId());
        Unit unit = unitService.selectByPrimaryKey(product.getUnitId());
        Warehouse warehouse = warehouseService.selectByPrimaryKey(warehouseId);
        item.put("productName", product.getName());
        item.put("productId", product.getId());
        item.put("productCode", product.getCode());
        item.put("productSpecification", product.getSpecification());
        item.put("productCategoryId", product.getProductCategoryId());
        item.put("productCategoryName", productCategory.getName());
        item.put("productUnitName", unit.getName());
        item.put("productUnitId", product.getUnitId());
        item.put("warehouseName", warehouse.getName());
        item.put("warehouseId", warehouse.getId());
        item.put("quantity", quantity);
    }

    /**
     * 获取盘点盘盈
     *
     * @param stockTakeItems 盘点明细列表
     * @return 盘盈列表
     */
    private List<Map<String, Object>> getInbounds(List<StockTakeItem> stockTakeItems) {
        List<Map<String, Object>> inbounds = new ArrayList<>();
        Map<String, Object> item;
        for (StockTakeItem stockTakeItem : stockTakeItems) {
            Integer systemQuantity = stockTakeItem.getSystemQuantity();
            Integer actualQuantity = stockTakeItem.getActualQuantity();
            if (actualQuantity - systemQuantity < 0) {
                item = new HashMap<>();
                // 盘亏
                this.getStockBoundsItem(stockTakeItem, item, systemQuantity - actualQuantity);
                inbounds.add(item);
            }
        }
        return inbounds;
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private OrderStatus state;

        private Long warehouseId;

        private String filter;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qStockTake.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qStockTake.accountBookId.eq(accountBookId));
            }
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
            return builder;
        }
    }
}
