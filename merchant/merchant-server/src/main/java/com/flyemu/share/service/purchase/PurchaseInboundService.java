package com.flyemu.share.service.purchase;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.purchase.PurchaseInboundDto;
import com.flyemu.share.dto.purchase.PurchaseInboundItemDto;
import com.flyemu.share.dto.purchase.PurchaseOrderDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.basic.PriceRecord;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.entity.purchase.PurchaseInboundItem;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseInboundItem;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import com.flyemu.share.form.PurchaseInboundForm;
import com.flyemu.share.repository.PurchaseInboundItemRepository;
import com.flyemu.share.repository.PurchaseInboundRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.PriceRecordService;
import com.flyemu.share.service.inventory.InventoryService;
import com.flyemu.share.service.setting.CodeSeedService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @功能描述: 采购入库表
 * @创建时间: 2025年02月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PurchaseInboundService extends AbsService {

    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QPurchaseInboundItem qPurchaseInboundItem = QPurchaseInboundItem.purchaseInboundItem;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;
    private final static QProduct qProduct = QProduct.product;
    private final static QProductCategory qProductCategory = QProductCategory.productCategory;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;

    private final PurchaseInboundRepository purchaseInboundRepository;
    private final PurchaseInboundItemRepository inboundItemRepository;

    private final PriceRecordService priceRecordService;
    private final CodeSeedService codeSeedService;

    private final InventoryService inventoryService;

    public PageResults<PurchaseInboundDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name, qMerchantUser.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder).orderBy(qPurchaseInbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<PurchaseInboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            PurchaseInboundDto dto = BeanUtil.toBean(tuple.get(qPurchaseInbound), PurchaseInboundDto.class);
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setCreatedName(tuple.get(qMerchantUser.name));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound.finalAmount.sum())
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qPurchaseInbound.createdBy))
                .where(query.builder).fetchFirst();
    }

    @Transactional
    public PurchaseInbound save(PurchaseInboundForm purchaseInboundForm, Long merchantId) {
        PurchaseInbound purchaseInbound = purchaseInboundForm.getPurchaseInbound();
        if (purchaseInbound.getId() != null) {
            PurchaseInbound original = purchaseInboundRepository.getById(purchaseInbound.getId());
            Assert.isFalse(original.getOrderStatus().equals(OrderStatus.已审核), "已审核订单不能更新~");
            BeanUtil.copyProperties(purchaseInbound, original, CopyOptions.create().ignoreNullValue());

            Set<Long> ids = new HashSet<>();
            Double secondarySum = 0.0;
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));
                if (d.getId() != null) {
                    ids.add(d.getId());
                }
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
                secondarySum += d.getSecondaryQuantity();
                //保存更新购货商品价格
                savePrice(d, purchaseInbound);
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            original.setSecondarySum(secondarySum);
            return purchaseInboundRepository.save(original);
        } else {
            purchaseInbound.setOrderNo(codeSeedService.generateCode(merchantId, "采购入库单"));
            purchaseInbound.setOrderStatus(OrderStatus.已保存);
            Double secondarySum = purchaseInboundForm.getPurchaseInboundItemList()
                    .stream()
                    .map(PurchaseInboundItem::getSecondaryQuantity)
                    .reduce(0.0, Double::sum);
            purchaseInbound.setSecondarySum(secondarySum);
            purchaseInbound = purchaseInboundRepository.save(purchaseInbound);
            for (PurchaseInboundItem d : purchaseInboundForm.getPurchaseInboundItemList()) {
                //计算基本单价
                d.setUnitPrice(BigDecimal.valueOf(NumberUtil.div(d.getSecondaryPrice(), d.getQuantity(), 2)));
                d.setAccountBookId(purchaseInbound.getAccountBookId());
                d.setPurchaseInboundId(purchaseInbound.getId());
                d.setMerchantId(merchantId);
                //保存更新购货商品价格
                savePrice(d, purchaseInbound);
            }
            inboundItemRepository.saveAll(purchaseInboundForm.getPurchaseInboundItemList());
            return purchaseInbound;
        }
    }

    private void savePrice(PurchaseInboundItem item, PurchaseInbound order) {
        PriceRecord priceRecord = new PriceRecord();
        priceRecord.setUnitPrice(item.getUnitPrice());
        priceRecord.setBaseUnitId(item.getBaseUnitId());
        priceRecord.setProductId(item.getProductId());
        priceRecord.setMerchantId(item.getMerchantId());
        priceRecord.setSupplierId(order.getSupplierId());
        priceRecord.setAccountBookId(order.getAccountBookId());
        priceRecord.setOrderId(order.getId());
        priceRecord.setPriceSource(PriceSource.最近采购价格);
        priceRecord.setPriceType(PriceType.最近采购价格);
        priceRecordService.savePriceRecord(priceRecord);
    }

    @Transactional
    public void delete(Long purchaseInboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qPurchaseInbound)
                .where(qPurchaseInbound.id.eq(purchaseInboundId).and(qPurchaseInbound.merchantId.eq(merchantId)).and(qPurchaseInbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PurchaseInbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPurchaseInbound).where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.accountBookId.eq(accountBookId))).fetch();
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        List<PurchaseInbound> orders = bqf.selectFrom(qPurchaseInbound).where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.id.in(ids))).fetch();
        Assert.isFalse(CollUtil.isEmpty(orders), "未找到数据~");
        List<Long> setIds = new ArrayList<>();
        if (OrderStatus.已审核.equals(state)) {
            for (PurchaseInbound order : orders) {
                if (OrderStatus.已保存.equals(order.getOrderStatus())) {
                    setIds.add(order.getId());
                } else {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                }
            }
        } else if (OrderStatus.已保存.equals(state)) {
            for (PurchaseInbound order : orders) {
                if (OrderStatus.已审核.equals(order.getOrderStatus())) {
                    setIds.add(order.getId());
                } else {
                    log.error("批量操作,状态不一致-----orderId:{},State:{}", order.getId(), order.getOrderStatus());
                }
            }
        }
        if (CollUtil.isNotEmpty(setIds)) {
            jqf.update(qPurchaseInbound)
                    .set(qPurchaseInbound.orderStatus, state)
                    .set(qPurchaseInbound.approvedAt, LocalDateTime.now()).
                    set(qPurchaseInbound.approvedBy, adminId)
                    .where(qPurchaseInbound.id.in(setIds))
                    .execute();
            // 设置入库明细数据
            this.purchaseInboundToInventory(state, setIds);
        }
    }

    private void purchaseInboundToInventory(OrderStatus state, List<Long> setIds) {
        setIds.forEach(id -> {
            purchaseInboundRepository.findById(id).ifPresent(purchaseInbound -> {
                List<Inventory> inventories = new ArrayList<>();
                List<InventoryItem> inventoryItems = new ArrayList<>();
                List<PurchaseInboundItem> inboundItems = inboundItemRepository.findByPurchaseInboundId(purchaseInbound.getId());
                //处理库存
                this.getComputedInventory(inboundItems, inventories, inventoryItems, purchaseInbound.getSupplierId(), purchaseInbound.getOrderNo());
                inventories.forEach(item -> {
                    if (OrderStatus.已审核.equals(state)) {
                        // 加库存
                        inventoryService.computedInventory(item, true, id, OperationType.采购入库, inventoryItems);
                    } else {
                        // 减库存
                        inventoryService.computedInventory(item, false, id, OperationType.采购入库, null);
                    }
                });
            });
        });
    }

    private void getComputedInventory(List<PurchaseInboundItem> inboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, Long supplierId, String orderNo) {
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        inboundItems.forEach(purchaseInboundItem -> {
            BigDecimal subtotal = purchaseInboundItem.getSubtotal();
            Double quantity = purchaseInboundItem.getQuantity();
            inventories.stream()
                    .filter(item -> item.getProductId().equals(purchaseInboundItem.getProductId())
                            && item.getWarehouseId().equals(purchaseInboundItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                BigDecimal totalCost = item.getTotalCost();
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal added = totalCost.add(subtotal)
                                        .setScale(2, RoundingMode.HALF_EVEN);
                                double parsed = Double.parseDouble(quantity.toString());
                                currentQuantity += (int) parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setWarehouseId(purchaseInboundItem.getWarehouseId());
                                inventory.setProductId(purchaseInboundItem.getProductId());
                                double parsed = Double.parseDouble(purchaseInboundItem.getQuantity().toString());
                                inventory.setCurrentQuantity((int) parsed);
                                inventory.setTotalCost(purchaseInboundItem.getSubtotal());
                                inventory.setMerchantId(purchaseInboundItem.getMerchantId());
                                inventory.setAccountBookId(purchaseInboundItem.getAccountBookId());
                                inventory.setBaseUnitId(purchaseInboundItem.getBaseUnitId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(purchaseInboundItem, supplierId, orderNo);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param purchaseInboundItem 入库明细
     * @param supplierId          供应商id
     * @return inventoryItem
     */
    private InventoryItem getInventoryItem(PurchaseInboundItem purchaseInboundItem, Long supplierId, String orderNo) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setWarehouseId(purchaseInboundItem.getWarehouseId());
        inventoryItem.setProductId(purchaseInboundItem.getProductId());
        double parsed = Double.parseDouble(purchaseInboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(purchaseInboundItem.getBaseUnitId());
        inventoryItem.setSupplierId(supplierId);
        inventoryItem.setOperationType(OperationType.采购入库);
        inventoryItem.setBaseUnitId(purchaseInboundItem.getBaseUnitId());
        inventoryItem.setOrderId(purchaseInboundItem.getPurchaseInboundId());
        inventoryItem.setBatchNumber(orderNo);
        inventoryItem.setMerchantId(purchaseInboundItem.getMerchantId());
        inventoryItem.setAccountBookId(purchaseInboundItem.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(purchaseInboundItem.getCreatedBy());
        inventoryItem.setUnitPrice(purchaseInboundItem.getUnitPrice());
        inventoryItem.setSubtotal(purchaseInboundItem.getSubtotal());
        return inventoryItem;
    }

    public Dict load(Long merchantId, Long orderId) {
        Tuple fetchFirst = jqf.selectFrom(qPurchaseInbound)
                .select(qPurchaseInbound, qSupplier.name)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qPurchaseInbound.supplierId))
                .where(qPurchaseInbound.merchantId.eq(merchantId).and(qPurchaseInbound.id.eq(orderId))).fetchFirst();

        QUnit qUnit1 = new QUnit("id");

        PurchaseOrderDto orderDto = BeanUtil.toBean(fetchFirst.get(qPurchaseInbound), PurchaseOrderDto.class);
        orderDto.setSupplierName(fetchFirst.get(qSupplier.name));
        ArrayList<PurchaseInboundItemDto> collect = jqf.selectFrom(qPurchaseInboundItem)
                .select(qPurchaseInboundItem, qProduct.code, qProduct.name, qWarehouse.name, qProductCategory.name,
                        qProduct.imgPath, qProduct.specification, qUnit.name, qUnit1.name, qProduct.specification)
                .leftJoin(qProduct).on(qProduct.id.eq(qPurchaseInboundItem.productId).and(qProduct.merchantId.eq(merchantId)))
                .leftJoin(qUnit).on(qUnit.id.eq(qPurchaseInboundItem.baseUnitId).and(qUnit.merchantId.eq(merchantId)))
                .leftJoin(qUnit1).on(qUnit1.id.eq(qPurchaseInboundItem.secondaryUnitId).and(qUnit1.merchantId.eq(merchantId)))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qPurchaseInboundItem.warehouseId).and(qWarehouse.merchantId.eq(merchantId)))
                .where(qPurchaseInboundItem.purchaseInboundId.eq(orderId).and(qPurchaseInboundItem.merchantId.eq(merchantId)))
                .orderBy(qPurchaseInboundItem.id.asc())
                .fetch().stream().collect(ArrayList::new, (list, tuple) -> {
                    PurchaseInboundItemDto dto = BeanUtil.toBean(tuple.get(qPurchaseInboundItem), PurchaseInboundItemDto.class);
                    dto.setProductCode(tuple.get(qProduct.code));
                    dto.setProductName(tuple.get(qProduct.name));
                    dto.setBaseUnitName(tuple.get(qUnit.name));
                    dto.setWarehouseName(tuple.get(qWarehouse.name));
                    dto.setSpec(tuple.get(qProduct.specification));
                    dto.setCategoryName(tuple.get(qProductCategory.name));
                    dto.setSecondaryUnitName(tuple.get(qUnit1.name));
                    list.add(dto);
                }, List::addAll);
        return Dict.create().set("purchaseInbound", orderDto).set("purchaseInboundItemList", collect);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();


        public void setState(OrderStatus state) {
            if (state != null) {
                builder.and(qPurchaseInbound.orderStatus.eq(state));
            }
        }

        public void setFilter(String filter) {
            if (StrUtil.isNotEmpty(filter)) {
                builder.and(qPurchaseInbound.orderNo.contains(filter).or(qSupplier.name.contains(filter)));
            }
        }

        public void setStart(LocalDate start) {
            if (start != null) {
                builder.and(qPurchaseInbound.inboundDate.goe(start));
            }
        }

        public void setEnd(LocalDate end) {
            if (end != null) {
                builder.and(qPurchaseInbound.inboundDate.loe(end));
            }
        }


        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPurchaseInbound.merchantId.eq(merchantId));
            }
        }

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qPurchaseInbound.supplierId.eq(supplierId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPurchaseInbound.accountBookId.eq(accountBookId));
            }
        }
    }
}
