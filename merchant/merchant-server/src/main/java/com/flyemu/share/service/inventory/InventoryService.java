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
import com.flyemu.share.entity.inventory.QInventoryItem;
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
    private final static QInventoryItem qInventoryItem = QInventoryItem.inventoryItem;

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

    /**
     * 调整库存数量
     * @param inventory 库存记录
     * @param quantity 调整数量（正数增加，负数减少）
     */
    @Transactional
    public void adjustQuantity(Inventory inventory, Integer quantity) {
        if (inventory == null) {
            throw new ServiceException("库存记录不存在");
        }
        Integer currentQty = inventory.getCurrentQuantity() != null ? inventory.getCurrentQuantity() : 0;
        Integer newQty = currentQty + quantity;
        if (newQty < 0) {
            throw new ServiceException("库存不足，当前库存：" + currentQty + "，调整数量：" + quantity);
        }
        inventory.setCurrentQuantity(newQty);
        inventoryRepository.save(inventory);
        log.info("库存调整：商品ID={}，仓库ID={}，货位ID={}，调整数量={}，调整后库存={}",
                inventory.getProductId(), inventory.getWarehouseId(), inventory.getLocationId(), quantity, newQty);
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
        // 如果有日期参数，按日期计算历史库存余额
        if (StrUtil.isNotBlank(query.getStart()) || StrUtil.isNotBlank(query.getEnd())) {
            return balanceByDate(page, query);
        }

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

    /**
     * 按日期计算历史库存余额
     */
    private PageResults<InventoryReportDto> balanceByDate(Page page, Query query) {
        // 查询截止到指定日期的所有库存明细，计算历史库存余额
        BooleanBuilder where = new BooleanBuilder();
        where.and(qInventoryItem.merchantId.eq(query.getMerchantId()));
        where.and(qInventoryItem.accountBookId.eq(query.getAccountBookId()));

        Date endDate = query.getEndDate();

        // 只过滤结束日期，查询截止到该日期的所有库存变动
        if (endDate != null) {
            where.and(qInventoryItem.inventoryDate.loe(Query.addTimeOfFinalMoment(endDate)));
        }

        // 应用其他过滤条件（仓库、产品、类别等）
        if (query.getWarehouseId() != null) {
            where.and(qInventoryItem.warehouseId.eq(query.getWarehouseId()));
        }
        if (query.getProductId() != null) {
            where.and(qInventoryItem.productId.eq(query.getProductId()));
        }
        if (StrUtil.isNotBlank(query.getWarehouseIds())) {
            where.and(qInventoryItem.warehouseId.in(Arrays.stream(query.getWarehouseIds().split(",")).map(Long::parseLong).toList()));
        }
        if (StrUtil.isNotBlank(query.getProductIds())) {
            where.and(qInventoryItem.productId.in(Arrays.stream(query.getProductIds().split(",")).map(Long::parseLong).toList()));
        }

        // 按商品和仓库分组，计算截止到指定日期的历史库存余额
        List<Tuple> results = bqf.selectFrom(qInventoryItem)
                .select(
                        qInventoryItem.productId,
                        qInventoryItem.warehouseId,
                        qInventoryItem.quantity.sum().as("totalQuantity"),
                        qInventoryItem.subtotal.sum().as("totalSubtotal")
                )
                .where(where)
                .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId)
                .fetch();

        // 获取商品信息
        Map<Long, InventoryReportDto> productMap = new LinkedHashMap<>();
        for (Tuple tuple : results) {
            Long productId = tuple.get(qInventoryItem.productId);
            Long warehouseId = tuple.get(qInventoryItem.warehouseId);
            Integer quantity = tuple.get(qInventoryItem.quantity.sum().as("totalQuantity"));
            BigDecimal subtotal = tuple.get(qInventoryItem.subtotal.sum().as("totalSubtotal"));

            if (!productMap.containsKey(productId)) {
                // 查询商品信息
                Product product = bqf.selectFrom(qProduct)
                        .where(qProduct.id.eq(productId))
                        .fetchFirst();
                if (product == null) continue;

                // 应用产品类别过滤
                if (query.getProductCategoryId() != null && (product.getProductCategoryId() == null ||
                        !product.getProductCategoryId().equals(query.getProductCategoryId()))) {
                    continue;
                }
                if (StrUtil.isNotBlank(query.getProductCategoryIds())) {
                    List<Long> categoryIds = Arrays.stream(query.getProductCategoryIds().split(",")).map(Long::parseLong).toList();
                    if (product.getProductCategoryId() == null || !categoryIds.contains(product.getProductCategoryId())) {
                        continue;
                    }
                }

                ProductCategory category = null;
                if (product.getProductCategoryId() != null) {
                    category = bqf.selectFrom(qProductCategory)
                            .where(qProductCategory.id.eq(product.getProductCategoryId()))
                            .fetchFirst();
                }

                Unit unit = null;
                if (product.getUnitId() != null) {
                    unit = bqf.selectFrom(qUnit)
                            .where(qUnit.id.eq(product.getUnitId()))
                            .fetchFirst();
                }

                // 应用名称/编码过滤
                if (StrUtil.isNotBlank(query.getFilter()) && StrUtil.isNotBlank(query.getFilter().trim())) {
                    String filter = query.getFilter().trim();
                    boolean match = product.getName().contains(filter) ||
                            product.getCode().contains(filter) ||
                            (category != null && category.getName().contains(filter)) ||
                            (product.getSpecification() != null && product.getSpecification().contains(filter));
                    if (!match) continue;
                }

                InventoryReportDto dto = new InventoryReportDto();
                dto.setProductId(productId);
                dto.setProductCode(product.getCode());
                dto.setProductName(product.getName());
                dto.setProductCategoryName(category != null ? category.getName() : "");
                dto.setProductSpecification(product.getSpecification());
                dto.setProductUnitName(unit != null ? unit.getName() : "");
                dto.setRetailCustomerPrice(product.getRetailCustomerPrice());
                dto.setPurchasePrice(product.getPurchasePrice());
                productMap.put(productId, dto);
            }

            // 设置仓库库存数据
            InventoryReportDto dto = productMap.get(productId);
            Warehouse warehouse = bqf.selectFrom(qWarehouse)
                    .where(qWarehouse.id.eq(warehouseId))
                    .fetchFirst();
            if (warehouse != null) {
                String warehouseField = warehouse.getCode() + "_" + warehouse.getId();
                // 这里需要动态设置仓库字段，但 InventoryReportDto 是固定的
                // 所以我们返回一个简化的结果
            }
        }

        // 分页处理
        List<InventoryReportDto> dtos = new ArrayList<>(productMap.values());
        int total = dtos.size();
        int fromIndex = Math.min(page.getOffset(), total);
        int toIndex = Math.min(fromIndex + page.getPageSize(), total);
        List<InventoryReportDto> pagedList = dtos.subList(fromIndex, toIndex);

        return new PageResults<>(pagedList, page, total);
    }

    public List<Inventory> balanceTotal(Query query) {
        // 如果有日期参数，按日期计算历史库存余额
        if (StrUtil.isNotBlank(query.getStart()) || StrUtil.isNotBlank(query.getEnd())) {
            return balanceTotalByDate(query);
        }

        return jqf.selectFrom(qInventory)
                .leftJoin(qWarehouse).on(qInventory.warehouseId.eq(qWarehouse.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .where(query.builders())
                .orderBy(qInventory.productId.desc())
                .fetch();
    }

    /**
     * 按日期计算历史库存余额总计
     */
    private List<Inventory> balanceTotalByDate(Query query) {
        BooleanBuilder where = new BooleanBuilder();
        where.and(qInventoryItem.merchantId.eq(query.getMerchantId()));
        where.and(qInventoryItem.accountBookId.eq(query.getAccountBookId()));

        Date endDate = query.getEndDate();

        // 只过滤结束日期，查询截止到该日期的所有库存变动
        if (endDate != null) {
            where.and(qInventoryItem.inventoryDate.loe(Query.addTimeOfFinalMoment(endDate)));
        }

        // 应用其他过滤条件
        if (query.getWarehouseId() != null) {
            where.and(qInventoryItem.warehouseId.eq(query.getWarehouseId()));
        }
        if (query.getProductId() != null) {
            where.and(qInventoryItem.productId.eq(query.getProductId()));
        }
        if (StrUtil.isNotBlank(query.getWarehouseIds())) {
            where.and(qInventoryItem.warehouseId.in(Arrays.stream(query.getWarehouseIds().split(",")).map(Long::parseLong).toList()));
        }
        if (StrUtil.isNotBlank(query.getProductIds())) {
            where.and(qInventoryItem.productId.in(Arrays.stream(query.getProductIds().split(",")).map(Long::parseLong).toList()));
        }

        // 按商品和仓库分组，计算截止到指定日期的历史库存余额
        List<Tuple> results = bqf.selectFrom(qInventoryItem)
                .select(
                        qInventoryItem.productId,
                        qInventoryItem.warehouseId,
                        qInventoryItem.quantity.sum().as("totalQuantity"),
                        qInventoryItem.subtotal.sum().as("totalSubtotal")
                )
                .where(where)
                .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId)
                .fetch();

        // 转换为 Inventory 对象
        List<Inventory> inventories = new ArrayList<>();
        for (Tuple tuple : results) {
            Inventory inventory = new Inventory();
            inventory.setProductId(tuple.get(qInventoryItem.productId));
            inventory.setWarehouseId(tuple.get(qInventoryItem.warehouseId));
            inventory.setCurrentQuantity(tuple.get(qInventoryItem.quantity.sum().as("totalQuantity")));
            inventory.setTotalCost(tuple.get(qInventoryItem.subtotal.sum().as("totalSubtotal")));
            inventories.add(inventory);
        }

        return inventories;
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

    /**
     * 查询库存成本详情（总成本、当前数量、平均成本）
     */
    public Map<String, Object> costDetail(Long productId, Long warehouseId, Long merchantId, Long accountBookId) {
        Map<String, Object> result = new HashMap<>();
        Inventory inventory = jqf.selectFrom(qInventory).where(qInventory.productId.eq(productId)
                .and(qInventory.warehouseId.eq(warehouseId)).and(qInventory.merchantId.eq(merchantId))
                .and(qInventory.accountBookId.eq(accountBookId))).fetchOne();
        if (inventory == null) {
            result.put("totalCost", BigDecimal.ZERO);
            result.put("currentQuantity", 0);
            result.put("averageCost", BigDecimal.ZERO);
        } else {
            result.put("totalCost", inventory.getTotalCost() != null ? inventory.getTotalCost() : BigDecimal.ZERO);
            result.put("currentQuantity", inventory.getCurrentQuantity() != null ? inventory.getCurrentQuantity() : 0);
            result.put("averageCost", inventory.getAverageCost() != null ? inventory.getAverageCost() : BigDecimal.ZERO);
        }
        return result;
    }

    /**
     * 查询尾差商品（数量为0但金额不为0）
     */
    public List<Map<String, Object>> tailDifference(Long merchantId, Long accountBookId) {
        List<Tuple> tuples = jqf.selectFrom(qInventory)
                .select(qInventory, qProduct.code, qProduct.name, qWarehouse.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventory.productId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventory.warehouseId))
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qInventory.currentQuantity.eq(0))
                        .and(qInventory.totalCost.ne(BigDecimal.ZERO)))
                .fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Tuple tuple : tuples) {
            Map<String, Object> item = new HashMap<>();
            Inventory inv = tuple.get(qInventory);
            item.put("productId", inv.getProductId());
            item.put("warehouseId", inv.getWarehouseId());
            item.put("productCode", tuple.get(qProduct.code));
            item.put("productName", tuple.get(qProduct.name));
            item.put("warehouseName", tuple.get(qWarehouse.name));
            item.put("totalCost", inv.getTotalCost());
            item.put("currentQuantity", 0);
            item.put("averageCost", BigDecimal.ZERO);
            result.add(item);
        }
        return result;
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

        private String start;

        private String end;

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

        public Date getStartDate() {
            if (StrUtil.isNotBlank(start)) {
                try {
                    java.time.LocalDate localDate = java.time.LocalDate.parse(start);
                    return Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
        }

        public Date getEndDate() {
            if (StrUtil.isNotBlank(end)) {
                try {
                    java.time.LocalDate localDate = java.time.LocalDate.parse(end);
                    return Date.from(localDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant());
                } catch (Exception e) {
                    return null;
                }
            }
            return null;
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
