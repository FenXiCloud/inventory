package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.InventoryTransferDto;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.InventoryTransferForm;
import com.flyemu.share.repository.InventoryTransferRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

import static com.flyemu.share.entity.inventory.QInventoryTransfer.inventoryTransfer;

/**
 * @功能描述: 调拨单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryTransferService extends AbsService {

    private final static QInventoryTransfer qInventoryTransfer = inventoryTransfer;

    private final InventoryTransferRepository inventoryTransferRepository;

    private final InventoryTransferItemService inventoryTransferItemService;

    private final InventoryService inventoryService;

    private final static QWarehouse toQWarehouse = new QWarehouse("to_warehouse");

    private final static QWarehouse formQWarehouse = new QWarehouse("form_warehouse");

    private final static QAdmin qAdmin = QAdmin.admin;


    public PageResults<InventoryTransferDto> query(Page page, InventoryTransferService.Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qInventoryTransfer)
                .select(qInventoryTransfer, toQWarehouse.name, formQWarehouse.name, qAdmin.name)
                .leftJoin(qAdmin).on(qAdmin.id.eq(qInventoryTransfer.createdBy))
                .leftJoin(toQWarehouse).on(toQWarehouse.id.eq(qInventoryTransfer.ToWarehouseId))
                .leftJoin(formQWarehouse).on(formQWarehouse.id.eq(qInventoryTransfer.FromWarehouseId))
                .where(query.builder).where(query.builders())
                .orderBy(qInventoryTransfer.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        List<InventoryTransferDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            InventoryTransferDto dto = BeanUtil.toBean(tuple.get(qInventoryTransfer), InventoryTransferDto.class);
            dto.setToWarehouseName(tuple.get(toQWarehouse.name));
            dto.setFromWarehouseName(tuple.get(formQWarehouse.name));
            dto.setCreatedByName(tuple.get(qAdmin.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public InventoryTransfer save(InventoryTransferForm inventoryTransferForm) {
        InventoryTransfer result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        InventoryTransfer inventoryTransfer = inventoryTransferForm.getInventoryTransfer();
        if (inventoryTransfer.getId() != null) {
            //更新
            InventoryTransfer original = inventoryTransferRepository.getById(inventoryTransfer.getId());
            BeanUtil.copyProperties(inventoryTransfer, original, CopyOptions.create().ignoreNullValue());
            result = inventoryTransferRepository.save(original);
        } else {
            inventoryTransfer.setCreatedAt(LocalDateTime.now());
            inventoryTransfer.setOrderNo(snowflakeGenerator.next().toString());
            result = inventoryTransferRepository.save(inventoryTransfer);
        }
        // 处理调拨单明细
        inventoryTransferItemService.generateInventoryTransferDetails(result, inventoryTransferForm.getInventoryTransferItems());
        return result;
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qInventoryTransfer)
                .where(qInventoryTransfer.id.eq(supplierFlowId).and(qInventoryTransfer.merchantId.eq(merchantId)).and(qInventoryTransfer.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<InventoryTransfer> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventoryTransfer).where(qInventoryTransfer.merchantId.eq(merchantId).and(qInventoryTransfer.accountBookId.eq(accountBookId))).fetch();
    }

    private static Date addTimeOfFinalMoment(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 23);
        calendar.add(Calendar.MINUTE, 59);
        calendar.add(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    @Transactional
    public void approve(Long id, ApproveType type, Long adminId) {
        InventoryTransfer inventoryTransfer = jqf.selectFrom(qInventoryTransfer).where(qInventoryTransfer.id.eq(id)).fetchOne();
        List<InventoryTransferItem> inventoryTransferItems = inventoryTransferItemService.findByInventoryTransferId(id);
        switch (type) {
            case AUDITS -> {
                //处理库存 （调入库存增加、调出库存减少）
                this.getComputedInventory(inventoryTransfer, inventoryTransferItems, false);
                inventoryTransfer.setOrderStatus(OrderStatus.已审核);
                inventoryTransfer.setApprovedBy(adminId);
                inventoryTransfer.setApprovedAt(LocalDateTime.now());
                inventoryTransferRepository.save(inventoryTransfer);
            }
            case ANTI_AUDIT -> {
                //处理库存（调入库存减少、调出库存增加）
                this.getComputedInventory(inventoryTransfer, inventoryTransferItems, true);
                jqf.delete(qInventoryTransfer).where(qInventoryTransfer.id.eq(id)).execute();
                inventoryTransferItemService.deleteByInventoryTransferId(id);
            }
            default -> {

            }
        }
    }

    /**
     * 计算库存
     *
     * @param inventoryTransfer      调拨单对象
     * @param inventoryTransferItems 调拨单明细对象
     * @param isRevoke               是否撤回
     */
    private void getComputedInventory(InventoryTransfer inventoryTransfer, List<InventoryTransferItem> inventoryTransferItems,
                                      boolean isRevoke) {
        List<Inventory> increaseInventory = new ArrayList<>();
        List<Inventory> reduceInventory = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        for (InventoryTransferItem inventoryTransferItem : inventoryTransferItems) {
            Long fromWarehouseId = inventoryTransferItem.getFromWarehouseId();
            Long toWarehouseId = inventoryTransferItem.getToWarehouseId();
            Long productId = inventoryTransferItem.getProductId();
            // 调出仓库不会为空，前端已控制
            Inventory fromInventory = inventoryService.findByWarehouseIdAndProductId(fromWarehouseId, productId);
            Double transferQuantity = inventoryTransferItem.getQuantity();
            BigDecimal subtotal = fromInventory.getAverageCost().multiply(new BigDecimal(transferQuantity)).setScale(2, RoundingMode.DOWN);
            // 调出仓库处理
            this.operateTransferItem(reduceInventory, fromWarehouseId, productId, fromInventory, transferQuantity, subtotal);
            // 调入仓库处理
            this.operateTransferItem(increaseInventory, toWarehouseId, productId, fromInventory, transferQuantity, subtotal);
            // 获取处理仓库明细
            InventoryItem inventoryItem = this.getInventoryItem(inventoryTransferItem, inventoryTransfer, fromInventory);
            inventoryItems.add(inventoryItem);
        }
        if (isRevoke) {
            increaseInventory.forEach(item -> {
                // 减库存
                inventoryService.computedInventory(item, false, inventoryTransfer.getId(), null, false);
            });
            reduceInventory.forEach(item -> {
                // 加库存
                inventoryService.computedInventory(item, true, inventoryTransfer.getId(), inventoryItems);
            });
            return;
        }
        reduceInventory.forEach(item -> {
            // 减库存
            inventoryService.computedInventory(item, false, inventoryTransfer.getId(), null, false);
        });
        increaseInventory.forEach(item -> {
            // 加库存
            inventoryService.computedInventory(item, true, inventoryTransfer.getId(), null);
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param inventoryTransferItem 调拨明细
     * @return inventoryItem 库存明细
     */
    private InventoryItem getInventoryItem(InventoryTransferItem inventoryTransferItem, InventoryTransfer inventoryTransfer,
                                           Inventory inventory) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(inventoryTransferItem.getProductId());
        inventoryItem.setWarehouseId(inventoryTransferItem.getToWarehouseId());
        inventoryItem.setQuantity(inventoryTransferItem.getQuantity().intValue());
        inventoryItem.setOperationType(OperationType.调拨);
        inventoryItem.setBaseUnitId(inventory.getBaseUnitId());
        inventoryItem.setOrderId(inventoryTransfer.getId());
        inventoryItem.setBatchNumber(inventoryTransfer.getOrderNo());
        inventoryItem.setMerchantId(inventoryTransfer.getMerchantId());
        inventoryItem.setAccountBookId(inventoryTransfer.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(inventoryTransfer.getCreatedBy());
        return inventoryItem;
    }


    /**
     * 处理初始化库存
     *
     * @param inventories      操作库存列表
     * @param warehouseId      仓库id
     * @param productId        商品id
     * @param fromInventory    操作库存
     * @param transferQuantity 转换数量
     * @param subtotal         价格
     */
    private void operateTransferItem(List<Inventory> inventories, Long warehouseId, Long productId,
                                     Inventory fromInventory, Double transferQuantity, BigDecimal subtotal) {
        inventories.stream().filter(item -> item.getProductId().equals(productId) &&
                item.getWarehouseId().equals(warehouseId)).findFirst().ifPresentOrElse(item -> {
            Integer currentQuantity = item.getCurrentQuantity();
            BigDecimal totalCost = item.getTotalCost();
            BigDecimal added = totalCost.add(subtotal)
                    .setScale(2, RoundingMode.DOWN);
            currentQuantity += transferQuantity.intValue();
            item.setCurrentQuantity(currentQuantity);
            item.setTotalCost(added);
        }, () -> {
            Inventory inventory = new Inventory();
            inventory.setProductId(productId);
            inventory.setWarehouseId(warehouseId);
            inventory.setCurrentQuantity(transferQuantity.intValue());
            inventory.setTotalCost(subtotal);
            inventory.setMerchantId(fromInventory.getMerchantId());
            inventory.setBaseUnitId(fromInventory.getBaseUnitId());
            inventory.setAccountBookId(fromInventory.getAccountBookId());
            inventories.add(inventory);
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param inventoryTransferItem 调拨单明细
     * @param baseUnitId            单位id
     * @param orderNo               订单号
     * @return inventoryItem
     */
    private InventoryItem getInventoryItem(InventoryTransferItem inventoryTransferItem,
                                           Long baseUnitId, String orderNo) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setWarehouseId(inventoryTransferItem.getToWarehouseId());
        inventoryItem.setProductId(inventoryTransferItem.getProductId());
        double parsed = Double.parseDouble(inventoryTransferItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(baseUnitId);
        inventoryItem.setOperationType(OperationType.调拨);
        inventoryItem.setOrderId(inventoryTransferItem.getInventoryTransferId());
        inventoryItem.setBatchNumber(orderNo);
        inventoryItem.setMerchantId(inventoryTransferItem.getMerchantId());
        inventoryItem.setAccountBookId(inventoryTransferItem.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(inventoryTransferItem.getCreatedBy());
        return inventoryItem;
    }

    public List<Map<String, Object>> load(Long id) {
        //todo 获取调拨单信息待优化
        return inventoryTransferRepository.findInventoryTransferById(id);
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
                builder.and(qInventoryTransfer.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventoryTransfer.accountBookId.eq(accountBookId));
            }
        }

        public BooleanBuilder builders() {
            if (start != null && end != null) {
                builder.and(qInventoryTransfer.transferDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qInventoryTransfer.transferDate.goe(start));
            }
            if (state != null) {
                builder.and(qInventoryTransfer.orderStatus.eq(state));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qInventoryTransfer.orderNo.contains(filter))
                        .or(qAdmin.name.contains(filter))
                        .or(formQWarehouse.name.contains(filter))
                        .or(toQWarehouse.name.contains(filter));
            }
            return builder;
        }

    }
}
