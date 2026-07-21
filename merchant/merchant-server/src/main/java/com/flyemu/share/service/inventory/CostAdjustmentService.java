package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.CostAdjustmentDto;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.CostAdjustmentForm;
import com.flyemu.share.repository.CostAdjustmentRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.StringTemplate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @功能描述: 成本调整单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CostAdjustmentService extends AbsService {

    private final CheckoutService checkoutService;
    private final static QCostAdjustment qCostAdjustment = QCostAdjustment.costAdjustment;

    private final static QCostAdjustmentItem qCostAdjustmentItem = QCostAdjustmentItem.costAdjustmentItem;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final static QUnit qUnit = QUnit.unit;

    private final static QAdmin qAdmin = QAdmin.admin;

    private final CostAdjustmentRepository costAdjustmentRepository;

    private final CostAdjustmentItemService costAdjustmentItemService;

    private final InventoryService inventoryService;

    public PageResults<CostAdjustmentDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf
                .selectFrom(qCostAdjustment)
                .select(qCostAdjustment, qAdmin.name)
                .leftJoin(qAdmin).on(qAdmin.id.eq(qCostAdjustment.createdBy))
                .where(query.builder)
                .where(query.builders())
                .orderBy(qCostAdjustment.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<CostAdjustmentDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            CostAdjustment costAdjustment1 = tuple.get(qCostAdjustment);
            CostAdjustmentDto costAdjustment = BeanUtil.toBean(costAdjustment1, CostAdjustmentDto.class);
            costAdjustment.setCreatedByName(tuple.get(qAdmin.name));
            dtos.add(costAdjustment);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public CostAdjustment save(CostAdjustmentForm costAdjustmentForm, Long merchantId) {
        CostAdjustment result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        CostAdjustment costAdjustment = costAdjustmentForm.getCostAdjustment();
        java.time.LocalDate __checkoutOrderDate = costAdjustment.getDjustmentDate() == null ? null : new java.sql.Date(costAdjustment.getDjustmentDate().getTime()).toLocalDate();
        checkoutService.assertEditable(costAdjustment.getMerchantId(), costAdjustment.getAccountBookId(), __checkoutOrderDate);
        costAdjustment.setMerchantId(merchantId);
        if (costAdjustment.getId() != null) {
            //更新
            CostAdjustment original = costAdjustmentRepository.getById(costAdjustment.getId());
            BeanUtil.copyProperties(costAdjustment, original, CopyOptions.create().ignoreNullValue());
            result = costAdjustmentRepository.save(original);
        } else {
            costAdjustment.setCreatedAt(LocalDateTime.now());
            costAdjustment.setOrderNo(snowflakeGenerator.next().toString());
            costAdjustment.setOrderStatus(OrderStatus.已保存);
            result = costAdjustmentRepository.save(costAdjustment);
        }
        // 保存明细
        BigDecimal adjustmentAmount = costAdjustmentItemService.generateCostAdjustmentDetails(result, costAdjustmentForm.getCostAdjustmentItems());
        result.setAdjustmentAmount(adjustmentAmount);
        costAdjustmentRepository.save(result);
        return result;
    }

    @Transactional
    public void delete(Long costAdjustmentId, Long merchantId, Long accountBookId) {
        jqf.delete(qCostAdjustment)
                .where(qCostAdjustment.id.eq(costAdjustmentId).and(qCostAdjustment.merchantId.eq(merchantId)).and(qCostAdjustment.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qCostAdjustmentItem)
                .where(qCostAdjustmentItem.costAdjustmentId.eq(costAdjustmentId))
                .execute();
    }

    public List<CostAdjustment> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCostAdjustment).where(qCostAdjustment.merchantId.eq(merchantId).and(qCostAdjustment.accountBookId.eq(accountBookId))).fetch();
    }

    public List<Map<String, Object>> load(Long merchantId, Long id) {
        StringTemplate dateExpressions = Expressions.
                stringTemplate("DATE_FORMAT({0},'%Y-%m-%d')", qCostAdjustment.djustmentDate);
        List<Tuple> fetch = jqf.selectFrom(qCostAdjustment)
                .select(
                        qCostAdjustment.id.as("id"),
                        dateExpressions.as("djustmentDate"),
                        qCostAdjustment.adjustmentType.as("adjustmentType"),
                        qCostAdjustment.orderStatus.as("orderStatus"),
                        qCostAdjustment.remarks.as("costRemarks"),
                        qCostAdjustmentItem.id.as("itemId"),
                        qProduct.id.as("productId"),
                        qProduct.imgPath.as("productUrl"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProduct.specification.as("productSpecification"),
                        qProductCategory.id.as("productCategoryId"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.unitId.as("productUnitId"),
                        qUnit.name.as("productUnitName"),
                        qWarehouse.id.as("warehouseId"),
                        qWarehouse.name.as("warehouseName"),
                        qCostAdjustmentItem.adjustmentAmount.as("adjustmentAmount"),
                        qCostAdjustmentItem.totalCost.as("totalCost"),
                        qAdmin.name.as("adminName"),
                        qCostAdjustmentItem.remarks.as("remarks")
                )
                .leftJoin(qCostAdjustmentItem).on(qCostAdjustmentItem.costAdjustmentId.eq(qCostAdjustment.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qCostAdjustmentItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qCostAdjustment.createdBy))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qCostAdjustmentItem.warehouseId))
                .where(qCostAdjustment.id.eq(id).and(qCostAdjustment.merchantId.eq(merchantId)))
                .fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> item;
        for (Tuple tuple : fetch) {
            item = new HashMap<>();
            item.put("id", tuple.get(qCostAdjustment.id.as("id")));
            item.put("djustmentDate", tuple.get(dateExpressions.as("djustmentDate")));
            item.put("orderStatus", tuple.get(qCostAdjustment.orderStatus.as("orderStatus")));
            item.put("adjustmentType", tuple.get(qCostAdjustment.adjustmentType.as("adjustmentType")));
            item.put("itemId", tuple.get(qCostAdjustmentItem.id.as("itemId")));
            item.put("productId", tuple.get(qProduct.id.as("productId")));
            item.put("costRemarks", tuple.get(qCostAdjustment.remarks.as("costRemarks")));
            item.put("productUrl", tuple.get(qProduct.imgPath.as("productUrl")));
            item.put("remarks", tuple.get(qCostAdjustmentItem.remarks.as("remarks")));
            item.put("productCode", tuple.get(qProduct.code.as("productCode")));
            item.put("productName", tuple.get(qProduct.name.as("productName")));
            item.put("productSpecification", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productUnitId", tuple.get(qProduct.unitId.as("productUnitId")));
            item.put("productCategoryId", tuple.get(qProductCategory.name.as("productCategoryName")));
            item.put("productCategoryName", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("warehouseId", tuple.get(qWarehouse.id.as("warehouseId")));
            item.put("productUnitName", tuple.get(qUnit.name.as("productUnitName")));
            item.put("warehouseName", tuple.get(qWarehouse.name.as("warehouseName")));
            item.put("adjustmentAmount", tuple.get(qCostAdjustmentItem.adjustmentAmount.as("adjustmentAmount")));
            item.put("totalCost", tuple.get(qCostAdjustmentItem.totalCost.as("totalCost")));
            item.put("adminName", tuple.get(qAdmin.name.as("adminName")));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        if (ids == null || ids.isEmpty()) {
            throw new ServiceException("未选择单据");
        }
        if (!OrderStatus.已审核.equals(state) && !OrderStatus.已保存.equals(state)) {
            throw new ServiceException("不支持的审核状态");
        }
        for (Long id : ids) {
            this.approve(id, state, adminId, merchantId);
        }
    }

    private void approve(Long id, OrderStatus state, Long adminId, Long merchantId) {
        CostAdjustment costAdjustment = jqf.selectFrom(qCostAdjustment)
                .where(qCostAdjustment.id.eq(id).and(qCostAdjustment.merchantId.eq(merchantId)))
                .fetchOne();
        if (costAdjustment == null) {
            throw new ServiceException("审核数据不存在～");
        }
        List<CostAdjustmentItem> costAdjustmentItems = costAdjustmentItemService.findByCostAdjustmentId(id);
        if (OrderStatus.已审核.equals(state)) {
            //处理成本
            this.processingCosts(costAdjustment, costAdjustmentItems, false);
            costAdjustment.setOrderStatus(OrderStatus.已审核);
            costAdjustment.setApprovedBy(adminId);
            costAdjustment.setApprovedAt(LocalDateTime.now());
            costAdjustmentRepository.save(costAdjustment);
        } else if (OrderStatus.已保存.equals(state)) {
            //处理成本
            this.processingCosts(costAdjustment, costAdjustmentItems, true);
            costAdjustment.setOrderStatus(OrderStatus.已保存);
            costAdjustment.setApprovedBy(adminId);
            costAdjustment.setApprovedAt(LocalDateTime.now());
            costAdjustmentRepository.save(costAdjustment);
        }
    }

    /**
     * 处理成本
     *
     * @param costAdjustment      调整单对象
     * @param costAdjustmentItems 调整单明细对象
     * @param inversely           是否相反处理
     */
    private void processingCosts(CostAdjustment costAdjustment,
                                 List<CostAdjustmentItem> costAdjustmentItems,
                                 Boolean inversely) {
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        for (CostAdjustmentItem costAdjustmentItem : costAdjustmentItems) {
            Inventory inventory = inventoryService.findByWarehouseIdAndProductId(costAdjustmentItem.getWarehouseId(), costAdjustmentItem.getProductId());
            // 库存不会为空，前端进行了控制
            if (inventory == null) {
                continue;
            }
            BigDecimal beforeTotalCost = inventory.getTotalCost();
            BigDecimal adjustmentAmount = costAdjustmentItem.getAdjustmentAmount();
            if (inversely) {
                // 取反
                adjustmentAmount = adjustmentAmount.negate();
            }
            BigDecimal totalCost = beforeTotalCost.add(adjustmentAmount).setScale(2, RoundingMode.HALF_EVEN);
            BigDecimal averageCost = inventory.getAverageCost();
            inventory.setTotalCost(totalCost);
            inventories.add(inventory);
            InventoryItem inventoryItem = getInventoryItem(costAdjustmentItem, costAdjustment, inventory, averageCost);
            inventoryItems.add(inventoryItem);
        }
        inventoryService.processingCosts(inventories, inventoryItems, inversely);
    }

    /**
     * 获取明细列表
     *
     * @param costAdjustmentItem 调拨明细
     * @return inventoryItem 库存明细
     */
    private InventoryItem getInventoryItem(CostAdjustmentItem costAdjustmentItem, CostAdjustment costAdjustment,
                                           Inventory inventory, BigDecimal averageCost) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(costAdjustmentItem.getProductId());
        inventoryItem.setWarehouseId(costAdjustmentItem.getWarehouseId());
        inventoryItem.setQuantity(0);
        inventoryItem.setInventoryDate(costAdjustment.getDjustmentDate());
        inventoryItem.setOperationType(OperationType.成本调整);
        inventoryItem.setBaseUnitId(inventory.getBaseUnitId());
        inventoryItem.setBatchNumber(costAdjustment.getOrderNo());
        inventoryItem.setOrderId(costAdjustment.getId());
        inventoryItem.setMerchantId(costAdjustment.getMerchantId());
        inventoryItem.setAccountBookId(costAdjustment.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(costAdjustment.getCreatedBy());
        inventoryItem.setSubtotal(costAdjustmentItem.getAdjustmentAmount());
        inventoryItem.setUnitPrice(inventory.getAverageCost());
        inventoryItem.setTotalCost(inventory.getTotalCost());
        inventoryItem.setCurrentQuantity(inventory.getCurrentQuantity());
        inventoryItem.setAverageCost(averageCost);
        return inventoryItem;
    }


    private static Date addTimeOfFinalMoment(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 23);
        calendar.add(Calendar.MINUTE, 59);
        calendar.add(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private OrderStatus state;

        private String filter;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCostAdjustment.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCostAdjustment.accountBookId.eq(accountBookId));
            }
        }

        public BooleanBuilder builders() {
            if (start != null && end != null) {
                builder.and(qCostAdjustment.djustmentDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qCostAdjustment.djustmentDate.goe(start));
            }
            if (state != null) {
                builder.and(qCostAdjustment.orderStatus.eq(state));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qCostAdjustment.orderNo.contains(filter))
                        .or(qAdmin.name.contains(filter));
            }
            return builder;
        }
    }
}
