package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.OtherInboundDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.ApproveType;
import com.flyemu.share.enums.InboundType;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.OtherInboundForm;
import com.flyemu.share.repository.OtherInboundRepository;
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
import java.util.concurrent.atomic.AtomicReference;

/**
 * @功能描述: 其他入库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherInboundService extends AbsService {

    private final static QOtherInbound qOtherInbound = QOtherInbound.otherInbound;

    private final OtherInboundItemService otherInboundItemService;

    private final InventoryService inventoryService;

    private final OtherInboundRepository otherInboundRepository;

    private final static QOtherInboundItem qOtherInboundItem = QOtherInboundItem.otherInboundItem;

    private final static QAdmin qAdmin = QAdmin.admin;

    private final static QCustomer qCustomer = QCustomer.customer;

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QUnit qUnit = QUnit.unit;

    private final static QWarehouse qWarehouse = QWarehouse.warehouse;


    public PageResults<OtherInboundDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qOtherInbound)
                .select(qOtherInbound, qAdmin.name, qCustomer.name, qCustomer.code, qSupplier.name, qSupplier.code,
                        qOtherInboundItem.quantity.sum().as("item_quantity"))
                .leftJoin(qOtherInboundItem).on(qOtherInboundItem.otherInboundId.eq(qOtherInbound.id))
                .leftJoin(qSupplier).on(qSupplier.id.eq(qOtherInbound.supplierId))
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOtherInbound.customerId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qOtherInbound.createdBy))
                .where(query.builder)
                .where(query.builders()).orderBy(qOtherInbound.id.desc())
                .groupBy(qOtherInbound.id)
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherInboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherInboundDto dto = BeanUtil.toBean(tuple.get(qOtherInbound), OtherInboundDto.class);
            dto.setQuantity(Objects.requireNonNull(tuple.get(qOtherInboundItem.quantity.sum().as("item_quantity"))).intValue());
            dto.setCreatedByName(tuple.get(qAdmin.name));
            dto.setCustomerCode(tuple.get(qCustomer.code));
            dto.setCustomerName(tuple.get(qCustomer.name));
            dto.setSupplierName(tuple.get(qSupplier.name));
            dto.setSupplierCode(tuple.get(qSupplier.code));
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherInbound save(OtherInboundForm otherInboundForm) {
        OtherInbound result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        OtherInbound otherInbound = otherInboundForm.getOtherInbound();
        if (otherInbound.getId() != null) {
            //更新
            OtherInbound original = otherInboundRepository.getById(otherInbound.getId());
            BeanUtil.copyProperties(otherInbound, original, CopyOptions.create().ignoreNullValue());
            result = otherInboundRepository.save(original);
        } else {
            otherInbound.setCreatedAt(LocalDateTime.now());
            otherInbound.setOrderNo(snowflakeGenerator.next().toString());
            result = otherInboundRepository.save(otherInbound);
        }
        // 入库明细
        BigDecimal totalAmount = otherInboundItemService.generateInboundDetails(result, otherInboundForm.getOtherInboundItems());
        result.setTotalAmount(totalAmount);
        otherInboundRepository.save(result);
        return result;
    }

    @Transactional
    public void delete(Long otherInboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherInboundItem)
                .where(qOtherInboundItem.otherInboundId.eq(otherInboundId).and(qOtherInboundItem.merchantId.eq(merchantId)).and(qOtherInboundItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qOtherInbound)
                .where(qOtherInbound.id.eq(otherInboundId).and(qOtherInbound.merchantId.eq(merchantId)).and(qOtherInbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherInbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherInbound).where(qOtherInbound.merchantId.eq(merchantId).and(qOtherInbound.accountBookId.eq(accountBookId))).fetch();
    }

    @Transactional
    public void approve(Long id, ApproveType type, Long adminId) {
        OtherInbound otherInbound = jqf.selectFrom(qOtherInbound).where(qOtherInbound.id.eq(id)).fetchOne();
        List<OtherInboundItem> otherInboundItems = otherInboundItemService.findByOtherInboundId(id);
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        switch (type) {
            case AUDITS -> {
                //处理库存
                this.getComputedInventory(otherInboundItems, inventories, inventoryItems, otherInbound.getSupplierId());
                inventories.forEach(item -> {
                    // 加库存
                    inventoryService.computedInventory(item, true, id, OperationType.入库, inventoryItems);
                });
                otherInbound.setOrderStatus(OrderStatus.已审核);
                otherInbound.setApprovedBy(adminId);
                otherInbound.setApprovedAt(LocalDateTime.now());
                otherInboundRepository.save(otherInbound);
            }
            case ANTI_AUDIT -> {
                //处理库存
                this.getComputedInventory(otherInboundItems, inventories, inventoryItems, otherInbound.getSupplierId());
                inventories.forEach(item -> {
                    // 减库存
                    inventoryService.computedInventory(item, false, id, OperationType.入库, null);
                });
                jqf.delete(qOtherInbound).where(qOtherInbound.id.eq(id)).execute();
                otherInboundItemService.deleteByOtherInboundId(id);
            }
            default -> {

            }
        }
    }

    /**
     * 统计操作的库存信息
     *
     * @param otherInboundItems 库存明细
     * @param inventories       操作库存
     * @param inventoryItems    操作库存明细
     * @param supplierId        供应商id
     */
    private void getComputedInventory(List<OtherInboundItem> otherInboundItems, List<Inventory> inventories,
                                      List<InventoryItem> inventoryItems, Long supplierId) {
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        otherInboundItems.forEach(otherInboundItem -> {
            BigDecimal subtotal = otherInboundItem.getSubtotal();
            Double quantity = otherInboundItem.getQuantity();
            inventories.stream()
                    .filter(item -> item.getProductId().equals(otherInboundItem.getProductId())
                            && item.getWarehouseId().equals(otherInboundItem.getWarehouseId()))
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
                                inventory.setWarehouseId(otherInboundItem.getWarehouseId());
                                inventory.setProductId(otherInboundItem.getProductId());
                                double parsed = Double.parseDouble(otherInboundItem.getQuantity().toString());
                                inventory.setCurrentQuantity((int) parsed);
                                inventory.setTotalCost(otherInboundItem.getSubtotal());
                                inventory.setMerchantId(otherInboundItem.getMerchantId());
                                inventory.setAccountBookId(otherInboundItem.getAccountBookId());
                                inventory.setBaseUnitId(otherInboundItem.getBaseUnitId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(otherInboundItem, supplierId);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param otherInboundItem 入库明细
     * @param supplierId       供应商id
     * @return inventoryItem
     */
    private InventoryItem getInventoryItem(OtherInboundItem otherInboundItem, Long supplierId) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setWarehouseId(otherInboundItem.getWarehouseId());
        inventoryItem.setProductId(otherInboundItem.getProductId());
        double parsed = Double.parseDouble(otherInboundItem.getQuantity().toString());
        inventoryItem.setQuantity((int) parsed);
        inventoryItem.setBaseUnitId(otherInboundItem.getBaseUnitId());
        inventoryItem.setSupplierId(supplierId);
        inventoryItem.setOperationType(OperationType.入库);
        inventoryItem.setBaseUnitId(otherInboundItem.getBaseUnitId());
        inventoryItem.setOrderId(otherInboundItem.getOtherInboundId());
        inventoryItem.setBatchNumber(otherInboundItem.getBatchNumber());
        inventoryItem.setMerchantId(otherInboundItem.getMerchantId());
        inventoryItem.setAccountBookId(otherInboundItem.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(otherInboundItem.getCreatedBy());
        inventoryItem.setUnitPrice(otherInboundItem.getUnitPrice());
        inventoryItem.setSubtotal(otherInboundItem.getSubtotal());
        return inventoryItem;
    }

    public List<Map<String, Object>> load(Long id) {
        StringTemplate dateExpressions = Expressions.
                stringTemplate("DATE_FORMAT({0},'%Y-%m-%d')", qOtherInbound.inboundDate);
        List<Tuple> fetch = jqf.selectFrom(qOtherInbound)
                .select(
                        qOtherInbound.id.as("id"),
                        dateExpressions.as("inboundDate"),
                        qOtherInbound.customerId.as("customerId"),
                        qOtherInbound.supplierId.as("supplierId"),
                        qOtherInbound.inboundType.as("inboundType"),
                        qOtherInbound.orderStatus.as("orderStatus"),
                        qOtherInbound.remarks.as("remarks"),
                        qOtherInboundItem.id.as("itemId"),
                        qProduct.id.as("productId"),
                        qProduct.imgPath.as("productUrl"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProduct.specification.as("productSpecification"),
                        qProduct.productCategoryId.as("productCategoryId"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.unitId.as("productUnitId"),
                        qUnit.name.as("productUnitName"),
                        qOtherInboundItem.warehouseId.as("warehouseId"),
                        qWarehouse.name.as("warehouseName"),
                        qOtherInboundItem.quantity.as("quantity"),
                        qOtherInboundItem.unitPrice.as("unitPrice"),
                        qOtherInboundItem.subtotal.as("subtotal"),
                        qAdmin.name.as("adminName")
                )
                .leftJoin(qOtherInboundItem).on(qOtherInboundItem.otherInboundId.eq(qOtherInbound.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qOtherInboundItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qOtherInbound.createdBy))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qOtherInboundItem.warehouseId))
                .where(qOtherInbound.id.eq(id)).fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> item;
        for (Tuple tuple : fetch) {
            item = new HashMap<>();
            item.put("id", tuple.get(qOtherInbound.id.as("id")));
            item.put("inboundDate", tuple.get(dateExpressions.as("inboundDate")));
            item.put("customerId", tuple.get(qOtherInbound.customerId.as("customerId")));
            item.put("supplierId", tuple.get(qOtherInbound.supplierId.as("supplierId")));
            item.put("inboundType", tuple.get(qOtherInbound.inboundType.as("inboundType")));
            item.put("remarks", tuple.get(qOtherInbound.remarks.as("remarks")));
            item.put("orderStatus", tuple.get(qOtherInbound.orderStatus.as("orderStatus")));
            item.put("itemId", tuple.get(qOtherInboundItem.id.as("itemId")));
            item.put("productId", tuple.get(qProduct.id.as("productId")));
            item.put("productUrl", tuple.get(qProduct.imgPath.as("productUrl")));
            item.put("productName", tuple.get(qProduct.name.as("productName")));
            item.put("productCode", tuple.get(qProduct.code.as("productCode")));
            item.put("productCategoryId", tuple.get(qProductCategory.name.as("productCategoryName")));
            item.put("productSpecification", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productCategoryName", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productUnitId", tuple.get(qProduct.unitId.as("productUnitId")));
            item.put("productUnitName", tuple.get(qUnit.name.as("productUnitName")));
            item.put("warehouseId", tuple.get(qOtherInboundItem.warehouseId.as("warehouseId")));
            item.put("warehouseName", tuple.get(qWarehouse.name.as("warehouseName")));
            item.put("quantity", tuple.get(qOtherInboundItem.quantity.as("quantity")));
            item.put("unitPrice", tuple.get(qOtherInboundItem.unitPrice.as("unitPrice")));
            item.put("subtotal", tuple.get(qOtherInboundItem.subtotal.as("subtotal")));
            item.put("adminName", tuple.get(qAdmin.name.as("adminName")));
            result.add(item);
        }
        return result;
    }

    public List<OtherInbound> findByStockTakeId(Long stockTakeId) {
        return jqf.selectFrom(qOtherInbound).where(qOtherInbound.stockTakeId.eq(stockTakeId)).fetch();
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private InboundType inboundType;

        private OrderStatus state;

        private String filter;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherInbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherInbound.accountBookId.eq(accountBookId));
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
                builder.and(qOtherInbound.inboundDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qOtherInbound.inboundDate.goe(start));
            }
            if (inboundType != null) {
                builder.and(qOtherInbound.inboundType.eq(inboundType));
            }
            if (state != null) {
                builder.and(qOtherInbound.orderStatus.eq(state));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qOtherInbound.orderNo.contains(filter))
                        .or(qAdmin.name.contains(filter))
                        .or(qCustomer.name.contains(filter));
            }
            return builder;
        }
    }
}
