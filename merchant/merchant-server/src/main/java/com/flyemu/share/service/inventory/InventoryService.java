package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.repository.InventoryItemRepository;
import com.flyemu.share.repository.InventoryRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    public void computedInventory(Inventory item, boolean increase, Long orderId, List<InventoryItem> inventoryItems) {
        this.computedInventory(item, increase, orderId, inventoryItems, true);
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
    public void computedInventory(Inventory item, boolean increase, Long orderId, List<InventoryItem> inventoryItems, boolean operateItems) {
        Inventory inventory = jqf.selectFrom(qInventory).where(qInventory.productId.eq(item.getProductId()))
                .where(qInventory.warehouseId.eq(item.getWarehouseId())).fetchFirst();
        if (inventory == null) {
            log.warn("计算库存获取库存对象失败 item:{},increase:{}", item, increase);
            //todo 获取库存失败，新增记录明细，待调整
            item.setCurrentQuantity(0);
            item.setAverageCost(BigDecimal.ZERO);
            item.setTotalCost(BigDecimal.ZERO);
            this.increaseInventory(item, inventoryItems, orderId);
            return;
        }
        Integer computedQuantity = item.getCurrentQuantity();
        BigDecimal computedCost = item.getTotalCost();
        Integer currentQuantity = inventory.getCurrentQuantity();
        BigDecimal totalCost = inventory.getTotalCost();
        if (increase) {
            currentQuantity += computedQuantity;
            totalCost = totalCost.add(computedCost).setScale(2, RoundingMode.DOWN);
            this.operateInventory(orderId, inventoryItems, inventory, currentQuantity, totalCost, operateItems);
            return;
        }
        //todo 负值库存待处理
        currentQuantity -= computedQuantity;
        totalCost = totalCost.subtract(computedCost).setScale(2, RoundingMode.DOWN);
        if (currentQuantity < 0) {
            currentQuantity = 0;
        }
        if (totalCost.compareTo(BigDecimal.ZERO) == 0) {
            totalCost = BigDecimal.ZERO;
        }
        this.operateInventory(orderId, inventoryItems, inventory, currentQuantity, totalCost, operateItems);
    }

    /**
     * 新增库存，同步处理记录明细
     *
     * @param item           库存记录
     * @param inventoryItems 明细
     * @param orderId        订单id
     */
    private void increaseInventory(Inventory item, List<InventoryItem> inventoryItems, Long orderId) {
        item.setUpdatedAt(LocalDateTime.now());
        inventoryRepository.save(item);
        if (inventoryItems == null) {
            inventoryItemService.deleteByOrderId(orderId);
        } else {
            inventoryItemService.batchInsertList(inventoryItems);
        }
    }

    /**
     * 操作库存
     *
     * @param orderId         订单id
     * @param inventoryItems  明细对象
     * @param inventory       库存对象
     * @param currentQuantity 当前库存
     * @param totalCost       总成本
     * @param operateItems    是否操作明细
     */
    private void operateInventory(Long orderId, List<InventoryItem> inventoryItems,
                                  Inventory inventory, Integer currentQuantity,
                                  BigDecimal totalCost, boolean operateItems) {
        BigDecimal averageCost = BigDecimal.ZERO;
        if (currentQuantity != 0) {
            averageCost = totalCost.divide(BigDecimal.valueOf(currentQuantity), 2, RoundingMode.DOWN);
        }
        jqf.update(qInventory).set(qInventory.currentQuantity, currentQuantity)
                .set(qInventory.totalCost, totalCost)
                .set(qInventory.averageCost, averageCost)
                .where(qInventory.id.eq(inventory.getId())).execute();
        if (!operateItems) {
            return;
        }
        if (inventoryItems == null) {
            inventoryItemService.deleteByOrderId(orderId);
        } else {
            inventoryItemService.batchInsertList(inventoryItems);
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

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Long warehouseId;

        private Long productId;

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
            return builder;
        }
    }
}
