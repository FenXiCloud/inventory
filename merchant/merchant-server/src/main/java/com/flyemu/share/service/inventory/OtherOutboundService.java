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
import com.flyemu.share.dto.OtherOutboundDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.OutboundType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.OtherOutboundForm;
import com.flyemu.share.repository.inventory.OtherOutboundItemRepository;
import com.flyemu.share.repository.inventory.OtherOutboundRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
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

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherOutboundService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QOtherOutbound qOtherOutbound = QOtherOutbound.otherOutbound;

    private final OtherOutboundRepository otherOutboundRepository;

    private final OtherOutboundItemService otherOutboundItemService;

    private final InventoryService inventoryService;
    private final CostingService costingService;
    private final OtherOutboundItemRepository otherOutboundItemRepository;

    private final static QOtherOutboundItem qOtherOutboundItem = QOtherOutboundItem.otherOutboundItem;

    private final static QCustomer qCustomer = QCustomer.customer;

    private final static QAdmin qAdmin = QAdmin.admin;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QUnit qUnit = QUnit.unit;

    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    public PageResults<OtherOutboundDto> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qOtherOutbound)
                .select(qOtherOutbound, qOtherOutboundItem.quantity.sum().as("item_quantity"))
                .leftJoin(qOtherOutboundItem).on(qOtherOutboundItem.otherOutboundId.eq(qOtherOutbound.id))
                .leftJoin(qCustomer).on(qOtherOutbound.customerId.eq(qCustomer.id))
                .leftJoin(qAdmin).on(qOtherOutbound.createdBy.eq(qAdmin.id))
                .where(query.builder)
                .where(query.builders())
                .orderBy(qOtherOutbound.id.desc())
                .groupBy(qOtherOutbound.id)
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherOutboundDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherOutboundDto dto = BeanUtil.toBean(tuple.get(qOtherOutbound), OtherOutboundDto.class);
            dto.setQuantity(Objects.requireNonNull(tuple.get(qOtherOutboundItem.quantity.sum().as("item_quantity"))).intValue());
            Long customerId = dto.getCustomerId();
            if (customerId != null) {
                Customer customer = jqf.selectFrom(qCustomer).where(qCustomer.id.eq(customerId)).fetchOne();
                if (customer != null) {
                    dto.setCustomerName(customer.getName());
                    dto.setCustomerCode(customer.getCode());
                }
            }
            Long createdBy = dto.getCreatedBy();
            Admin admin = jqf.selectFrom(qAdmin).where(qAdmin.id.eq(createdBy)).fetchOne();
            if (admin != null) {
                dto.setCreatedByName(admin.getName());
            }
            dtos.add(dto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherOutbound save(OtherOutboundForm otherOutboundForm, Long merchantId) {
        OtherOutbound result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        OtherOutbound otherOutbound = otherOutboundForm.getOtherOutbound();
        java.time.LocalDate __checkoutOrderDate = otherOutbound.getInboundDate() == null ? null : new java.sql.Date(otherOutbound.getInboundDate().getTime()).toLocalDate();
        checkoutService.assertEditable(otherOutbound.getMerchantId(), otherOutbound.getAccountBookId(), __checkoutOrderDate);
        otherOutbound.setMerchantId(merchantId);
        if (otherOutbound.getId() != null) {
            //更新
            OtherOutbound original = otherOutboundRepository.getById(otherOutbound.getId());
            BeanUtil.copyProperties(otherOutbound, original, CopyOptions.create().ignoreNullValue());
            result = otherOutboundRepository.save(original);
        } else {
            otherOutbound.setCreatedAt(LocalDateTime.now());
            otherOutbound.setOrderNo(snowflakeGenerator.next().toString());
            otherOutbound.setOrderStatus(OrderStatus.已保存);
            result = otherOutboundRepository.save(otherOutbound);
        }
        // 出库明细
        BigDecimal totalAmount = otherOutboundItemService.generateOutboundDetails(result, otherOutboundForm.getOtherOutboundItems());
        result.setTotalAmount(totalAmount);
        otherOutboundRepository.save(result);
        return result;
    }

    @Transactional
    public void delete(Long otherOutboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherOutboundItem)
                .where(qOtherOutboundItem.otherOutboundId.eq(otherOutboundId)
                        .and(qOtherOutboundItem.merchantId.eq(merchantId))
                        .and(qOtherOutboundItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qOtherOutbound)
                .where(qOtherOutbound.id.eq(otherOutboundId).and(qOtherOutbound.merchantId.eq(merchantId)).and(qOtherOutbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherOutbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherOutbound).where(qOtherOutbound.merchantId.eq(merchantId).and(qOtherOutbound.accountBookId.eq(accountBookId))).fetch();
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
        OtherOutbound otherOutbound = jqf.selectFrom(qOtherOutbound)
                .where(qOtherOutbound.id.eq(id).and(qOtherOutbound.merchantId.eq(merchantId)))
                .fetchOne();
        if (otherOutbound == null) {
            throw new ServiceException("审核数据不存在～");
        }
        otherOutbound = otherOutboundItemService.recalculateAmount(otherOutbound);
        OutboundType outboundType = otherOutbound.getOutboundType();
        OperationType operationType;
        if (outboundType.equals(OutboundType.其他出库)) {
            operationType = OperationType.其他出库;
        } else {
            operationType = OperationType.盘亏出库;
        }
        List<OtherOutboundItem> otherOutboundItems = otherOutboundItemService.findByOtherOutboundId(id);
        List<Inventory> inventories = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();
        if (OrderStatus.已审核.equals(state)) {
            applyIssueCost(otherOutbound, otherOutboundItems, operationType);
            this.getComputedInventory(otherOutboundItems, inventories, operationType, inventoryItems, otherOutbound);
            inventories.forEach(item -> {
                inventoryService.computedInventory(item, false, id, operationType, inventoryItems);
            });
            otherOutbound.setOrderStatus(OrderStatus.已审核);
            otherOutbound.setApprovedBy(adminId);
            otherOutbound.setApprovedAt(LocalDateTime.now());
            otherOutboundRepository.save(otherOutbound);
        } else if (OrderStatus.已保存.equals(state)) {
            this.getComputedInventory(otherOutboundItems, inventories, operationType, inventoryItems, otherOutbound);
            inventories.forEach(item -> {
                inventoryService.computedInventory(item, true, id, operationType, null);
            });
            costingService.reverseIssue(id, operationType, otherOutbound.getMerchantId(), otherOutbound.getAccountBookId());
            clearIssueCost(otherOutboundItems);
            otherOutbound.setOrderStatus(OrderStatus.已保存);
            otherOutbound.setApprovedBy(adminId);
            otherOutbound.setApprovedAt(LocalDateTime.now());
            otherOutboundRepository.save(otherOutbound);
        }
    }

    private void applyIssueCost(OtherOutbound otherOutbound, List<OtherOutboundItem> items, OperationType operationType) {
        for (OtherOutboundItem line : items) {
            int qty = line.getQuantity() == null ? 0 : line.getQuantity().intValue();
            CostingService.IssueRequest req = new CostingService.IssueRequest();
            req.setProductId(line.getProductId());
            req.setWarehouseId(line.getWarehouseId());
            req.setQty(qty);
            req.setOrderId(otherOutbound.getId());
            req.setOrderType(operationType);
            req.setItemId(line.getId());
            req.setMerchantId(otherOutbound.getMerchantId());
            req.setAccountBookId(otherOutbound.getAccountBookId());
            CostingService.IssueResult result = costingService.issue(req);
            line.setCostPrice(result.getCostPrice());
            line.setCostAmount(result.getCostAmount());
        }
        otherOutboundItemRepository.saveAll(items);
    }

    private void clearIssueCost(List<OtherOutboundItem> items) {
        for (OtherOutboundItem line : items) {
            line.setCostPrice(null);
            line.setCostAmount(null);
        }
        otherOutboundItemRepository.saveAll(items);
    }

    /**
     * 统计操作的库存信息
     *
     * @param otherOutboundItems 库存明细
     * @param inventories        操作库存
     * @param inventoryItems     操作库存明细
     */
    private void getComputedInventory(List<OtherOutboundItem> otherOutboundItems,
                                      List<Inventory> inventories,
                                      OperationType operationType,
                                      List<InventoryItem> inventoryItems,
                                      OtherOutbound otherOutbound) {
        AtomicReference<Inventory> inventoryAtomicReference = new AtomicReference<>();
        AtomicReference<InventoryItem> inventoryItemAtomicReference = new AtomicReference<>();
        otherOutboundItems.forEach(otherOutboundItem -> {
            BigDecimal quantity = otherOutboundItem.getQuantity();
            BigDecimal subtotal = otherOutboundItem.getCostAmount() != null
                    ? otherOutboundItem.getCostAmount() : otherOutboundItem.getSubtotal();
            if (subtotal == null) {
                Inventory inv = inventoryService.findByWarehouseIdAndProductId(
                        otherOutboundItem.getWarehouseId(), otherOutboundItem.getProductId());
                BigDecimal avg = inv != null && inv.getAverageCost() != null ? inv.getAverageCost() : BigDecimal.ZERO;
                subtotal = avg.multiply(quantity == null ? BigDecimal.ZERO : quantity)
                        .setScale(2, RoundingMode.HALF_EVEN);
            }
            BigDecimal finalSubtotal = subtotal;
            inventories.stream()
                    .filter(item -> item.getProductId().equals(otherOutboundItem.getProductId())
                            && item.getWarehouseId().equals(otherOutboundItem.getWarehouseId()))
                    .findFirst()
                    .ifPresentOrElse(
                            item -> {
                                Integer currentQuantity = item.getCurrentQuantity();
                                BigDecimal totalCost = item.getTotalCost();
                                BigDecimal added = totalCost.add(finalSubtotal)
                                        .setScale(2, RoundingMode.HALF_EVEN);
                                int parsed = quantity.intValue();
                                currentQuantity += parsed;
                                item.setCurrentQuantity(currentQuantity);
                                item.setTotalCost(added);
                            }, () -> {
                                Inventory inventory = new Inventory();
                                inventory.setProductId(otherOutboundItem.getProductId());
                                inventory.setWarehouseId(otherOutboundItem.getWarehouseId());
                                int parsed = otherOutboundItem.getQuantity().intValue();
                                inventory.setCurrentQuantity(parsed);
                                inventory.setTotalCost(finalSubtotal);
                                inventory.setMerchantId(otherOutboundItem.getMerchantId());
                                inventory.setBaseUnitId(otherOutboundItem.getBaseUnitId());
                                inventory.setAccountBookId(otherOutboundItem.getAccountBookId());
                                inventoryAtomicReference.set(inventory);
                                inventories.add(inventoryAtomicReference.get());
                            });
            InventoryItem inventoryItem = getInventoryItem(otherOutboundItem, otherOutbound, operationType, finalSubtotal);
            inventoryItemAtomicReference.set(inventoryItem);
            inventoryItems.add(inventoryItemAtomicReference.get());
        });
    }

    /**
     * 获取库存明细列表
     *
     * @param otherOutboundItem 出库明细
     * @return inventoryItem
     */
    private InventoryItem getInventoryItem(OtherOutboundItem otherOutboundItem, OtherOutbound otherOutbound,
                                           OperationType operationType, BigDecimal costAmount) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(otherOutboundItem.getProductId());
        inventoryItem.setWarehouseId(otherOutboundItem.getWarehouseId());
        int parsed = otherOutboundItem.getQuantity().intValue();
        inventoryItem.setQuantity(parsed);
        inventoryItem.setBaseUnitId(otherOutboundItem.getBaseUnitId());
        inventoryItem.setOperationType(operationType);
        inventoryItem.setOrderId(otherOutboundItem.getOtherOutboundId());
        inventoryItem.setMerchantId(otherOutboundItem.getMerchantId());
        inventoryItem.setBatchNumber(otherOutboundItem.getBatchNumber());
        inventoryItem.setAccountBookId(otherOutboundItem.getAccountBookId());
        inventoryItem.setCustomerId(otherOutbound.getCustomerId());
        inventoryItem.setInventoryDate(otherOutbound.getInboundDate());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(otherOutboundItem.getCreatedBy());
        inventoryItem.setUnitPrice(otherOutboundItem.getCostPrice() != null
                ? otherOutboundItem.getCostPrice() : otherOutboundItem.getUnitPrice());
        inventoryItem.setSubtotal(costAmount);
        return inventoryItem;
    }

    public List<Map<String, Object>> load(Long merchantId, Long id) {
        StringTemplate dateExpressions = Expressions.
                stringTemplate("DATE_FORMAT({0},'%Y-%m-%d')", qOtherOutbound.inboundDate);
        List<Tuple> fetch = jqf.selectFrom(qOtherOutbound)
                .select(
                        qOtherOutbound.id.as("id"),
                        dateExpressions.as("inboundDate"),
                        qOtherOutbound.customerId.as("customerId"),
                        qOtherOutbound.outboundType.as("outboundType"),
                        qOtherOutbound.stockTakeId.as("stockTakeId"),
                        qOtherOutbound.orderStatus.as("orderStatus"),
                        qOtherOutboundItem.id.as("itemId"),
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
                        qOtherOutboundItem.quantity.as("quantity"),
                        qOtherOutboundItem.unitPrice.as("unitPrice"),
                        qOtherOutboundItem.subtotal.as("subtotal"),
                        qAdmin.name.as("adminName"),
                        qOtherOutbound.remarks.as("remarks")
                )
                .leftJoin(qOtherOutboundItem).on(qOtherOutboundItem.otherOutboundId.eq(qOtherOutbound.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qOtherOutboundItem.productId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qOtherOutbound.createdBy))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qOtherOutboundItem.warehouseId))
                .where(qOtherOutbound.id.eq(id).and(qOtherOutbound.merchantId.eq(merchantId)))
                .fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> item;
        for (Tuple tuple : fetch) {
            item = new HashMap<>();
            item.put("id", tuple.get(qOtherOutbound.id.as("id")));
            item.put("inboundDate", tuple.get(dateExpressions.as("inboundDate")));
            item.put("customerId", tuple.get(qOtherOutbound.customerId.as("customerId")));
            item.put("stockTakeId", tuple.get(qOtherOutbound.stockTakeId.as("stockTakeId")));
            item.put("orderStatus", tuple.get(qOtherOutbound.orderStatus.as("orderStatus")));
            item.put("outboundType", tuple.get(qOtherOutbound.outboundType.as("outboundType")));
            item.put("itemId", tuple.get(qOtherOutboundItem.id.as("itemId")));
            item.put("productId", tuple.get(qProduct.id.as("productId")));
            item.put("remarks", tuple.get(qOtherOutbound.remarks.as("remarks")));
            item.put("productUrl", tuple.get(qProduct.imgPath.as("productUrl")));
            item.put("productName", tuple.get(qProduct.name.as("productName")));
            item.put("productCode", tuple.get(qProduct.code.as("productCode")));
            item.put("productSpecification", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productCategoryId", tuple.get(qProductCategory.name.as("productCategoryName")));
            item.put("productUnitId", tuple.get(qProduct.unitId.as("productUnitId")));
            item.put("productCategoryName", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productUnitName", tuple.get(qUnit.name.as("productUnitName")));
            item.put("warehouseId", tuple.get(qWarehouse.id.as("warehouseId")));
            item.put("warehouseName", tuple.get(qWarehouse.name.as("warehouseName")));
            item.put("quantity", tuple.get(qOtherOutboundItem.quantity.as("quantity")));
            item.put("unitPrice", tuple.get(qOtherOutboundItem.unitPrice.as("unitPrice")));
            item.put("adminName", tuple.get(qAdmin.name.as("adminName")));
            item.put("subtotal", tuple.get(qOtherOutboundItem.subtotal.as("subtotal")));
            result.add(item);
        }
        return result;
    }

    private static Date addTimeOfFinalMoment(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 23);
        calendar.add(Calendar.MINUTE, 59);
        calendar.add(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public List<OtherOutbound> findByStockTakeId(Long stockTakeId) {
        return jqf.selectFrom(qOtherOutbound).where(qOtherOutbound.stockTakeId.eq(stockTakeId)).fetch();
    }

    @Data
    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private OutboundType outboundType;

        private OrderStatus state;

        private String filter;

        private String productIds;

        private String warehouseIds;

        private String customerIds;

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qOtherOutbound.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qOtherOutbound.accountBookId, accountBookId);
        }

        public BooleanBuilder builders() {
            if (start != null && end != null) {
                builder.and(qOtherOutbound.inboundDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qOtherOutbound.inboundDate.goe(start));
            }
            if (outboundType != null) {
                builder.and(qOtherOutbound.outboundType.eq(outboundType));
            }
            if (state != null) {
                builder.and(qOtherOutbound.orderStatus.eq(state));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qOtherOutbound.orderNo.contains(filter))
                        .or(qCustomer.name.contains(filter))
                        .or(qAdmin.name.contains(filter));
            }
            if (StrUtil.isNotBlank(productIds)) {
                builder.and(qOtherOutboundItem.productId.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                builder.and(qOtherOutboundItem.warehouseId.in(Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(customerIds)) {
                builder.and(qOtherOutbound.customerId.in(Arrays.stream(customerIds.split(",")).map(Long::parseLong).toList()));
            }
            return builder;
        }
    }
}
