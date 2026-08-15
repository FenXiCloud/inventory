package com.flyemu.share.service.inventory;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.InventoryReportDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.inventory.InventoryRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.NativeQuery;
import org.hibernate.transform.Transformers;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryService extends BaseService {

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
        if (item.getProductId() == null || item.getWarehouseId() == null) {
            throw new ServiceException("库存计算失败：产品或仓库不能为空");
        }
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
        // 减少库存
        currentQuantity -= computedQuantity;
        totalCost = totalCost.subtract(computedCost).setScale(2, RoundingMode.HALF_EVEN);
        Long accountBookId = inventory.getAccountBookId() != null ? inventory.getAccountBookId() : item.getAccountBookId();
        if (!allowNegativeStock(accountBookId)) {
            if (currentQuantity < 0) {
                currentQuantity = 0;
            }
            if (totalCost.compareTo(BigDecimal.ZERO) < 1) {
                totalCost = BigDecimal.ZERO;
            }
        }
        this.operateInventory(orderId, operationType, inventoryItems, inventory, currentQuantity, totalCost, operateItems);
    }

    private boolean allowNegativeStock(Long accountBookId) {
        if (accountBookId == null) {
            return false;
        }
        AccountBookParameters params = bqf.selectFrom(QAccountBookParameters.accountBookParameters)
                .where(QAccountBookParameters.accountBookParameters.accountBookId.eq(Math.toIntExact(accountBookId)))
                .fetchFirst();
        return params != null && params.getAvailableInventory() != null && params.getAvailableInventory() == 1;
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
                item.setFirstSort(false);
                if (item.getProductId().equals(inventory.getProductId()) && item.getWarehouseId().equals(inventory.getWarehouseId())) {
                    item.setCurrentQuantity(currentQuantity);
                    item.setTotalCost(totalCost);
                    item.setAverageCost(averageCost);
                    insertList.add(item);
                }
            }
            if (!insertList.isEmpty()) {
                inventoryItemService.batchInsertList(insertList);
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

    public List<Map<String, Object>> products(Long warehouseId, String warehouseIds, Long productId, String filter, Long accountBookId, Long merchantId) {
        String productSql = InventoryRepository.PRODUCT_SQL;
        int index = 3;
        int productIndex = 3;
        int warehouseIndex = 3;
        int warehousesIndex = 3;
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
        if (StrUtil.isNotBlank(warehouseIds)) {
            productSql += " AND ji.warehouse_id IN (?" + index + ") ";
            warehousesIndex = index;
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
        if (StrUtil.isNotBlank(warehouseIds)) {
            nativeQuery.setParameter(warehousesIndex, Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList());
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
    public PageResults<InventoryReportDto> balance(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qInventory)
                .select(
                        qProduct.id.as("productId"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.specification.as("productSpecification"),
                        qUnit.name.as("productUnitName"),
                        qProduct.retailCustomerPrice.as("retailCustomerPrice"),
                        qProduct.purchasePrice.as("purchasePrice")
                )
                .leftJoin(qProduct).on(qInventory.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventory.warehouseId))
                .where(query.builders()).where(qProduct.id.isNotNull())
                .groupBy(
                        qProduct.id,
                        qProduct.code,
                        qProduct.name,
                        qProductCategory.name,
                        qProduct.specification,
                        qUnit.name,
                        qProduct.retailCustomerPrice,
                        qProduct.purchasePrice
                )
                .orderBy(qProduct.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        List<InventoryReportDto> dtos = new ArrayList<>();
        InventoryReportDto dto;
        for (Tuple tuple : fetchPage) {
            dto = new InventoryReportDto();
            dto.setProductId(tuple.get(qProduct.id.as("productId")));
            dto.setProductCode(tuple.get(qProduct.code.as("productCode")));
            dto.setProductName(tuple.get(qProduct.name.as("productName")));
            dto.setProductCategoryName(tuple.get(qProductCategory.name.as("productCategoryName")));
            dto.setProductSpecification(tuple.get(qProduct.specification.as("productSpecification")));
            dto.setProductUnitName(tuple.get(qUnit.name.as("productUnitName")));
            dto.setRetailCustomerPrice(tuple.get(qProduct.retailCustomerPrice.as("retailCustomerPrice")));
            dto.setPurchasePrice(tuple.get(qProduct.purchasePrice.as("purchasePrice")));
            dtos.add(dto);
        }
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public List<Inventory> balanceTotal(Query query) {
        return jqf.selectFrom(qInventory)
                .leftJoin(qWarehouse).on(qInventory.warehouseId.eq(qWarehouse.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builders())
                .orderBy(qInventory.productId.desc())
                .fetch();
    }

    /**
     * 按产品聚合销售加权均价（销售出库 − 销售退货，按各自 quantity 加权合并）
     * @return Map<productId, unitPrice>
     */
    @SuppressWarnings("unchecked")
    public Map<Long, BigDecimal> productSalesPrices(Query query) {
        Map<Long, BigDecimal> result = new HashMap<>();
        Long merchantId = query.getMerchantId();
        Long accountBookId = query.getAccountBookId();
        if (merchantId == null || accountBookId == null) {
            return result;
        }
        // 销售出库：按产品聚合 quantity 和 subtotal
        String outSql = """
            SELECT soi.product_id, SUM(soi.quantity), SUM(soi.subtotal)
            FROM jxc_sales_outbound_item soi
            WHERE soi.merchant_id = ?1 AND soi.account_book_id = ?2
            GROUP BY soi.product_id
            """;
        Map<Long, BigDecimal[]> outMap = new HashMap<>(); // productId -> [qty, subtotal]
        List<Object[]> outRows = entityManager.createNativeQuery(outSql)
                .setParameter(1, merchantId)
                .setParameter(2, accountBookId)
                .getResultList();
        for (Object[] row : outRows) {
            Long pid = ((Number) row[0]).longValue();
            BigDecimal qty = new BigDecimal(row[1].toString());
            BigDecimal subtotal = new BigDecimal(row[2].toString());
            outMap.put(pid, new BigDecimal[]{qty, subtotal});
        }
        // 销售退货：按产品聚合 quantity 和 subtotal
        String retSql = """
            SELECT sri.product_id, SUM(sri.quantity), SUM(sri.subtotal)
            FROM jxc_sales_return_item sri
            WHERE sri.merchant_id = ?1 AND sri.account_book_id = ?2
            GROUP BY sri.product_id
            """;
        Map<Long, BigDecimal[]> retMap = new HashMap<>(); // productId -> [qty, subtotal]
        List<Object[]> retRows = entityManager.createNativeQuery(retSql)
                .setParameter(1, merchantId)
                .setParameter(2, accountBookId)
                .getResultList();
        for (Object[] row : retRows) {
            Long pid = ((Number) row[0]).longValue();
            BigDecimal qty = new BigDecimal(row[1].toString());
            BigDecimal subtotal = new BigDecimal(row[2].toString());
            retMap.put(pid, new BigDecimal[]{qty, subtotal});
        }
        // 合并：净额加权均价 = (outSubtotal - retSubtotal) / (outQty - retQty)
        Set<Long> allProductIds = new HashSet<>();
        allProductIds.addAll(outMap.keySet());
        allProductIds.addAll(retMap.keySet());
        for (Long pid : allProductIds) {
            BigDecimal[] out = outMap.getOrDefault(pid, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal[] ret = retMap.getOrDefault(pid, new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal outQty = out[0], outSubtotal = out[1];
            BigDecimal retQty = ret[0], retSubtotal = ret[1];
            BigDecimal netQty = outQty.subtract(retQty);
            BigDecimal netSubtotal = outSubtotal.subtract(retSubtotal);
            if (netQty.compareTo(BigDecimal.ZERO) > 0) {
                result.put(pid, netSubtotal.divide(netQty, 2, RoundingMode.HALF_UP));
            } else if (outQty.compareTo(BigDecimal.ZERO) > 0) {
                result.put(pid, outSubtotal.divide(outQty, 2, RoundingMode.HALF_UP));
            }
        }
        return result;
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

    //@Scheduled(cron = "0 0 1 1 * ?") // 每月第一天第一分钟执行
    @Transactional
    public void initialInventory() {
        // 1、获取商品前一个月的库存数据
        List<Product> fetch = jqf.selectFrom(qProduct).fetch();
        List<InventoryItem> insertInventoryItems = new ArrayList<>();
        for (Product product : fetch) {
            List<Inventory> inventories = inventoryRepository.findByProductId(product.getId());
            if (inventories != null && !inventories.isEmpty()) {
                Integer quantity = 0;
                BigDecimal totalCost = BigDecimal.ZERO;
                for (Inventory inventory : inventories) {
                    quantity += inventory.getCurrentQuantity();
                    totalCost = totalCost.add(inventory.getTotalCost());
                }
                BigDecimal averageCost = totalCost.divide(new BigDecimal(quantity), 2, RoundingMode.HALF_UP);
                InventoryItem inventoryItem = new InventoryItem();
                inventoryItem.setProductId(product.getId());
                inventoryItem.setCurrentQuantity(quantity);
                inventoryItem.setTotalCost(totalCost);
                inventoryItem.setAverageCost(averageCost);
                inventoryItem.setOperationType(OperationType.期初余额);
                inventoryItem.setMerchantId(product.getMerchantId());
                inventoryItem.setAccountBookId(product.getAccountBookId());
                inventoryItem.setCreatedAt(LocalDateTime.now());
                inventoryItem.setCreatedBy(-1L);
                inventoryItem.setFirstSort(true);
                insertInventoryItems.add(inventoryItem);
            } else {
                InventoryItem inventoryItem = new InventoryItem();
                inventoryItem.setProductId(product.getId());
                inventoryItem.setOperationType(OperationType.期初余额);
                inventoryItem.setAccountBookId(product.getAccountBookId());
                inventoryItem.setMerchantId(product.getMerchantId());
                inventoryItem.setCreatedAt(LocalDateTime.now());
                inventoryItem.setCreatedBy(-1L);
                inventoryItem.setFirstSort(true);
                insertInventoryItems.add(inventoryItem);
            }
        }
        if (!insertInventoryItems.isEmpty()) {
            inventoryItemService.batchInsertList(insertInventoryItems);
        }
    }

    /**
     * 获取库存中的商品列表
     *
     * @param merchantId    商户id
     * @param accountBookId 账号id
     * @param warehouseId   仓库id
     * @return list
     */
    public List<Product> selectProduct(Long merchantId, Long accountBookId, Long warehouseId) {
        return jqf.selectFrom(qInventory)
                .select(qProduct)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qInventory.warehouseId.eq(warehouseId)))
                .orderBy(qInventory.productId.desc())
                .fetch();
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private Long warehouseId;

        private Long productId;

        private Long productCategoryId;

        private String filter;

        private String productIds;

        private String productCategoryIds;

        private String warehouseIds;

        private Long merchantId;

        private Long accountBookId;

        public void setMerchantId(Long merchantId) {
            this.merchantId = merchantId;
            TenantFilters.merchant(builder, qInventory.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            this.accountBookId = accountBookId;
            TenantFilters.accountBook(builder, qInventory.accountBookId, accountBookId);
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
            BooleanBuilder where = new BooleanBuilder(builder);
            if (warehouseId != null) {
                where.and(qInventory.warehouseId.eq(warehouseId));
            }
            if (productId != null) {
                where.and(qInventory.productId.eq(productId));
            }
            if (productCategoryId != null) {
                where.and(qProductCategory.id.eq(productCategoryId));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                where.and(qProduct.name.contains(filter)
                        .or(qProduct.code.contains(filter))
                        .or(qProductCategory.name.contains(filter))
                        .or(qProduct.specification.contains(filter)));
            }
            if (StrUtil.isNotBlank(productCategoryIds)) {
                where.and(qProduct.productCategoryId.in(Arrays.stream(productCategoryIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(productIds)) {
                where.and(qProduct.id.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                where.and(qWarehouse.id.in(Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList()));
            }
            return where;
        }
    }
}
