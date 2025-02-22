package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.InventoryReportDto;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.repository.InventoryRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @功能描述: 库存余额表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService extends AbsService {

    private final static QInventory qInventory = QInventory.inventory;

    private final InventoryRepository inventoryRepository;

    private final InventoryItemService inventoryItemService;

    @PersistenceContext
    private EntityManager entityManager;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final static QUnit qUnit = QUnit.unit;

    public PageResults<Inventory> query(Page page, Query query) {
        PagedList<Inventory> fetchPage = bqf.selectFrom(qInventory).where(query.builder).where(query.builders())
                .orderBy(qInventory.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<Inventory> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            Inventory inventory1 = tuple;
            Inventory inventory = BeanUtil.toBean(inventory1, Inventory.class);
            dtos.add(inventory);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public Inventory save(Inventory inventory) {
        if (inventory.getId() != null) {
            //更新
            Inventory original = inventoryRepository.getById(inventory.getId());
            BeanUtil.copyProperties(inventory, original, CopyOptions.create().ignoreNullValue());
            return inventoryRepository.save(original);
        }
        return inventoryRepository.save(inventory);
    }

    @Transactional
    public void delete(Long inventoryId, Long merchantId, Long accountBookId) {
        jqf.delete(qInventory)
                .where(qInventory.id.eq(inventoryId).and(qInventory.merchantId.eq(merchantId)).and(qInventory.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Inventory> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventory).where(qInventory.merchantId.eq(merchantId).and(qInventory.accountBookId.eq(accountBookId))).fetch();
    }

    /**
     * 计算库存
     *
     * @param item           库存对象
     * @param increase       是否添加
     * @param orderId        订单id
     * @param inventoryItems 库存明细
     */
    @Transactional
    public void computedInventory(Inventory item, boolean increase, Long orderId, OperationType operationType, List<InventoryItem> inventoryItems) {
        this.computedInventory(item, increase, orderId, operationType, inventoryItems, true);
    }

    /**
     * 计算库存
     *
     * @param item           库存对象
     * @param increase       是否添加
     * @param orderId        订单id
     * @param inventoryItems 库存明细
     * @param operateItems   是否操作明细
     */
    @Transactional
    public void computedInventory(Inventory item, boolean increase, Long orderId, OperationType operationType, List<InventoryItem> inventoryItems, boolean operateItems) {
        Inventory inventory = jqf.selectFrom(qInventory).where(qInventory.productId.eq(item.getProductId()))
                .where(qInventory.warehouseId.eq(item.getWarehouseId())).fetchFirst();
        if (inventory == null) {
            inventory = new Inventory();
            log.warn("计算库存获取库存对象失败 item:{},increase:{}", item, increase);
            //todo 获取库存失败，新增记录明细，待调整
            inventory.setCurrentQuantity(0);
            inventory.setBaseUnitId(item.getBaseUnitId());
            inventory.setUpdatedAt(LocalDateTime.now());
            inventory.setProductId(item.getProductId());
            inventory.setWarehouseId(item.getWarehouseId());
            inventory.setMerchantId(item.getMerchantId());
            inventory.setAccountBookId(item.getAccountBookId());
            inventory.setAverageCost(BigDecimal.ZERO);
            inventory.setTotalCost(BigDecimal.ZERO);
        }
        Integer computedQuantity = item.getCurrentQuantity();
        BigDecimal computedCost = item.getTotalCost();
        Integer currentQuantity = inventory.getCurrentQuantity();
        BigDecimal totalCost = inventory.getTotalCost();
        if (increase) {
            currentQuantity += computedQuantity;
            totalCost = totalCost.add(computedCost).setScale(2, RoundingMode.HALF_EVEN);
            this.operateInventory(orderId, operationType, inventoryItems, inventory, currentQuantity, totalCost, operateItems);
            return;
        }
        //todo 负值库存待处理
        currentQuantity -= computedQuantity;
        totalCost = totalCost.subtract(computedCost).setScale(2, RoundingMode.HALF_EVEN);
        if (currentQuantity < 0) {
            currentQuantity = 0;
        }
        if (totalCost.compareTo(BigDecimal.ZERO) < 1) {
            totalCost = BigDecimal.ZERO;
        }
        this.operateInventory(orderId, operationType, inventoryItems, inventory, currentQuantity, totalCost, operateItems);
    }

    /**
     * 操作库存
     *
     * @param orderId         订单id
     * @param inventoryItems  明细对象
     * @param operationType   明细类型
     * @param inventory       库存对象
     * @param currentQuantity 当前库存
     * @param totalCost       总成本
     * @param operateItems    是否操作明细
     */
    private void operateInventory(Long orderId, OperationType operationType, List<InventoryItem> inventoryItems,
                                  Inventory inventory, Integer currentQuantity,
                                  BigDecimal totalCost, boolean operateItems) {
        BigDecimal averageCost = BigDecimal.ZERO;
        if (currentQuantity != 0) {
            averageCost = totalCost.divide(BigDecimal.valueOf(currentQuantity), 2, RoundingMode.HALF_EVEN);
        }
        inventory.setCurrentQuantity(currentQuantity);
        inventory.setTotalCost(totalCost);
        inventory.setAverageCost(averageCost);
        inventoryRepository.save(inventory);
        if (!operateItems) {
            return;
        }
        if (inventoryItems == null) {
            inventoryItemService.deleteByOrderId(orderId, operationType);
        } else {
            List<InventoryItem> insertList = new ArrayList<>();
            for (InventoryItem item : inventoryItems) {
                if (item.getProductId().equals(inventory.getProductId()) && item.getWarehouseId().equals(inventory.getWarehouseId())) {
                    item.setCurrentQuantity(currentQuantity);
                    item.setTotalCost(totalCost);
                    item.setAverageCost(averageCost);
                    insertList.add(item);
                }
            }
            if (!insertList.isEmpty()) {
                inventoryItemService.batchInsertList(inventoryItems);
            }
        }
    }

    /**
     * 商品是否存在库存
     *
     * @param productId     商品id
     * @param warehouseId   仓库id
     * @param merchantId    商户id
     * @param accountBookId 账号id
     * @return true/false
     */
    public Boolean exist(Long productId, Long warehouseId, Long merchantId, Long accountBookId) {
        Inventory inventory = jqf.selectFrom(qInventory).where(qInventory.productId.eq(productId)
                .and(qInventory.warehouseId.eq(warehouseId)).and(qInventory.merchantId.eq(merchantId))
                .and(qInventory.accountBookId.eq(accountBookId))).fetchOne();
        if (inventory == null) {
            return false;
        }
        Integer currentQuantity = inventory.getCurrentQuantity();
        return currentQuantity != null && currentQuantity > 0;
    }

    /**
     * 根据仓库id和产品id获取库存信息
     *
     * @param warehouseId 仓库id
     * @param productId   产品id
     * @return inventory
     */
    public Inventory findByWarehouseIdAndProductId(Long warehouseId, Long productId) {
        return jqf.selectFrom(qInventory).where(qInventory.warehouseId.eq(warehouseId))
                .where(qInventory.productId.eq(productId)).fetchOne();
    }

    public List<Map<String, Object>> products(Long warehouseId, Long productId, String filter, Long accountBookId, Long merchantId) {
        String productSql = InventoryRepository.PRODUCT_SQL;
        int index = 3;
        int productIndex = 3;
        int warehouseIndex = 3;
        int filterIndex = 3;
        if (productId != null) {
            productSql += " AND ji.product_id= ?" + index;
            index++;
        }
        if (warehouseId != null) {
            productSql += " AND ji.warehouse_id= ?" + index;
            warehouseIndex = index;
            index++;
        }
        if (StringUtils.hasText(filter)) {
            productSql += " AND (jp.`name` LIKE CONCAT('%', ?" + index + ", '%') OR jp.`code` LIKE CONCAT('%', ?" + index + ", '%'))";
            filterIndex = index;
        }
        jakarta.persistence.Query nativeQuery = entityManager.createNativeQuery(productSql);
        nativeQuery.setParameter(1, accountBookId);
        nativeQuery.setParameter(2, merchantId);
        if (productId != null) {
            nativeQuery.setParameter(productIndex, productId);
        }
        if (warehouseId != null) {
            nativeQuery.setParameter(warehouseIndex, warehouseId);
        }
        if (StringUtils.hasText(filter)) {
            nativeQuery.setParameter(filterIndex, filter);
        }
        nativeQuery.unwrap(NativeQuery.class)
                .setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);
        return nativeQuery.getResultList();
    }

    /**
     * 库存余额统计
     */
    public PageResults<InventoryReportDto> report(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qInventory)
                .select(
                        qInventory.productId.count(),
                        qProduct.id.as("productId"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.specification.as("productSpecification"),
                        qUnit.name.as("productUnitName")
                )
                .leftJoin(qProduct).on(qInventory.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventory.warehouseId))
                .where(query.builders())
                .groupBy(qProduct.id)
                .orderBy(qProduct.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        List<InventoryReportDto> dtos = new ArrayList<>();
        InventoryReportDto dto;
        Long totalCount = 0L;
        for (Tuple tuple : fetchPage) {
            dto = new InventoryReportDto();
            dto.setProductId(tuple.get(qProduct.id.as("productId")));
            dto.setProductCode(tuple.get(qProduct.code.as("productCode")));
            dto.setProductName(tuple.get(qProduct.name.as("productName")));
            dto.setProductCategoryName(tuple.get(qProductCategory.name.as("productCategoryName")));
            dto.setProductSpecification(tuple.get(qProduct.specification.as("productSpecification")));
            dto.setProductUnitName(tuple.get(qUnit.name.as("productUnitName")));
            dtos.add(dto);
        }
        if (!fetchPage.isEmpty()) {
            totalCount = fetchPage.get(0).get(qInventory.productId.count());
        }
        if (totalCount == null) {
            totalCount = 0L;
        }
        return new PageResults<>(dtos, page, totalCount);
    }

    public List<Inventory> reportInventory(Query query) {
        return jqf.selectFrom(qInventory)
                .leftJoin(qWarehouse).on(qInventory.warehouseId.eq(qWarehouse.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builders())
                .orderBy(qInventory.productId.desc())
                .fetch();
    }

    public BigDecimal totalCost(Long productId, Long warehouseId, Long merchantId, Long accountBookId) {
        Inventory inventory = jqf.selectFrom(qInventory).where(qInventory.productId.eq(productId)
                .and(qInventory.warehouseId.eq(warehouseId)).and(qInventory.merchantId.eq(merchantId))
                .and(qInventory.accountBookId.eq(accountBookId))).fetchOne();
        if (inventory == null) {
            return BigDecimal.ZERO;
        }
        return inventory.getTotalCost();
    }

    public void processingCosts(List<Inventory> inventories, List<InventoryItem> inventoryItems, Boolean inversely) {
        inventoryRepository.saveAll(inventories);
        if (inversely) {
            // 删除明细
            List<Long> orderIds = inventoryItems.stream().map(InventoryItem::getOrderId).toList();
            orderIds.forEach(orderId -> {
                inventoryItemService.deleteByOrderId(orderId, OperationType.成本调整);
            });
            return;
        }
        if (inventoryItems != null && !inventoryItems.isEmpty()) {
            inventoryItemService.batchInsertList(inventoryItems);
        }
    }


    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Long warehouseId;

        private Long productId;

        private Long productCategoryId;

        private String filter;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qInventory.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventory.accountBookId.eq(accountBookId));
            }
        }

        public BooleanBuilder builders() {
            if (warehouseId != null) {
                builder.and(qInventory.warehouseId.eq(warehouseId));
            }
            if (productId != null) {
                builder.and(qInventory.productId.eq(productId));
            }
            if (productCategoryId != null) {
                builder.and(qProductCategory.id.eq(productCategoryId));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qProduct.name.contains(filter))
                        .or(qProduct.code.contains(filter))
                        .or(qProductCategory.name.contains(filter))
                        .or(qProduct.specification.contains(filter));
            }
            return builder;
        }
    }
}
