package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.generator.SnowflakeGenerator;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.InventoryTransferDto;
import com.flyemu.share.entity.basic.QProduct;
import com.flyemu.share.entity.basic.QProductCategory;
import com.flyemu.share.entity.basic.QUnit;
import com.flyemu.share.entity.basic.QWarehouse;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.setting.QAdmin;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.InventoryTransferForm;
import com.flyemu.share.repository.InventoryTransferRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
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

    private final CheckoutService checkoutService;
    private final static QInventoryTransfer qInventoryTransfer = inventoryTransfer;

    private final InventoryTransferRepository inventoryTransferRepository;

    private final InventoryTransferItemService inventoryTransferItemService;

    private final InventoryService inventoryService;
    private final CostingService costingService;

    private final static QWarehouse toQWarehouse = new QWarehouse("to_warehouse");

    private final static QWarehouse formQWarehouse = new QWarehouse("form_warehouse");

    private final static QAdmin qAdmin = QAdmin.admin;

    private final static QInventoryTransferItem qInventoryTransferItem = QInventoryTransferItem.inventoryTransferItem;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QUnit qUnit = QUnit.unit;

    private final static QInventory qInventory = QInventory.inventory;

    public PageResults<InventoryTransferDto> query(Page page, InventoryTransferService.Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qInventoryTransfer)
                .select(qInventoryTransfer, toQWarehouse.name, formQWarehouse.name, qAdmin.name)
                .leftJoin(qAdmin).on(qAdmin.id.eq(qInventoryTransfer.createdBy))
                .leftJoin(toQWarehouse).on(toQWarehouse.id.eq(qInventoryTransfer.ToWarehouseId))
                .leftJoin(formQWarehouse).on(formQWarehouse.id.eq(qInventoryTransfer.FromWarehouseId))
                .leftJoin(qInventoryTransferItem).on(qInventoryTransferItem.inventoryTransferId.eq(qInventoryTransfer.id))
                .where(query.builder).where(query.builders())
                .groupBy(qInventoryTransfer.id)
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
    public InventoryTransfer save(InventoryTransferForm inventoryTransferForm, Long merchantId) {
        InventoryTransfer result;
        SnowflakeGenerator snowflakeGenerator = new SnowflakeGenerator();
        InventoryTransfer inventoryTransfer = inventoryTransferForm.getInventoryTransfer();
        java.time.LocalDate __checkoutOrderDate = inventoryTransfer.getTransferDate() == null ? null : new java.sql.Date(inventoryTransfer.getTransferDate().getTime()).toLocalDate();
        checkoutService.assertEditable(inventoryTransfer.getMerchantId(), inventoryTransfer.getAccountBookId(), __checkoutOrderDate);
        inventoryTransfer.setMerchantId(merchantId);
        if (inventoryTransfer.getId() != null) {
            //更新
            InventoryTransfer original = inventoryTransferRepository.getById(inventoryTransfer.getId());
            BeanUtil.copyProperties(inventoryTransfer, original, CopyOptions.create().ignoreNullValue());
            result = inventoryTransferRepository.save(original);
        } else {
            inventoryTransfer.setCreatedAt(LocalDateTime.now());
            inventoryTransfer.setOrderNo(snowflakeGenerator.next().toString());
            inventoryTransfer.setOrderStatus(OrderStatus.已保存);
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
        InventoryTransfer inventoryTransfer = jqf.selectFrom(qInventoryTransfer)
                .where(qInventoryTransfer.id.eq(id).and(qInventoryTransfer.merchantId.eq(merchantId)))
                .fetchOne();
        if (inventoryTransfer == null) {
            throw new ServiceException("审核数据不存在～");
        }
        List<InventoryTransferItem> inventoryTransferItems = inventoryTransferItemService.findByInventoryTransferId(id);
        if (OrderStatus.已审核.equals(state)) {
            //处理库存 （调入库存增加、调出库存减少）
            this.getComputedInventory(inventoryTransfer, inventoryTransferItems, false);
            inventoryTransfer.setOrderStatus(OrderStatus.已审核);
            inventoryTransfer.setApprovedBy(adminId);
            inventoryTransfer.setApprovedAt(LocalDateTime.now());
            inventoryTransferRepository.save(inventoryTransfer);
        } else if (OrderStatus.已保存.equals(state)) {
            //处理库存（调入库存减少、调出库存增加）
            this.getComputedInventory(inventoryTransfer, inventoryTransferItems, true);
            inventoryTransfer.setOrderStatus(OrderStatus.已保存);
            inventoryTransfer.setApprovedBy(adminId);
            inventoryTransfer.setApprovedAt(LocalDateTime.now());
            inventoryTransferRepository.save(inventoryTransfer);
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
        if (isRevoke) {
            // 先回补调入仓批次校验，再回补库存余额，最后回补调出仓批次
            costingService.reverseReceipt(inventoryTransfer.getId(), OperationType.调拨入库,
                    inventoryTransfer.getMerchantId(), inventoryTransfer.getAccountBookId());
        }

        List<Inventory> increaseInventory = new ArrayList<>();
        List<Inventory> reduceInventory = new ArrayList<>();
        List<InventoryItem> inventoryItems = new ArrayList<>();

        for (InventoryTransferItem inventoryTransferItem : inventoryTransferItems) {
            Long fromWarehouseId = inventoryTransferItem.getFromWarehouseId();
            Long toWarehouseId = inventoryTransferItem.getToWarehouseId();
            Long productId = inventoryTransferItem.getProductId();
            Inventory fromInventory = inventoryService.findByWarehouseIdAndProductId(fromWarehouseId, productId);
            Inventory toInventory = inventoryService.findByWarehouseIdAndProductId(toWarehouseId, productId);
            if (fromInventory == null) {
                throw new ServiceException("调出仓库存不存在");
            }
            if (toInventory == null) {
                toInventory = new Inventory();
                toInventory.setProductId(productId);
                toInventory.setWarehouseId(toWarehouseId);
                toInventory.setAccountBookId(fromInventory.getAccountBookId());
                toInventory.setMerchantId(fromInventory.getMerchantId());
                toInventory.setAverageCost(BigDecimal.ZERO);
                toInventory.setTotalCost(BigDecimal.ZERO);
                toInventory.setCurrentQuantity(0);
                toInventory.setBaseUnitId(fromInventory.getBaseUnitId());
            }
            Double transferQuantity = inventoryTransferItem.getQuantity();
            int qty = transferQuantity == null ? 0 : (int) Math.round(transferQuantity);
            if (qty <= 0) {
                continue;
            }
            BigDecimal subtotal;
            BigDecimal unitCost = BigDecimal.ZERO;
            if (!isRevoke) {
                CostingService.IssueRequest issueReq = new CostingService.IssueRequest();
                issueReq.setProductId(productId);
                issueReq.setWarehouseId(fromWarehouseId);
                issueReq.setQty(qty);
                issueReq.setOrderId(inventoryTransfer.getId());
                issueReq.setOrderType(OperationType.调拨出库);
                issueReq.setItemId(inventoryTransferItem.getId());
                issueReq.setMerchantId(inventoryTransfer.getMerchantId());
                issueReq.setAccountBookId(inventoryTransfer.getAccountBookId());
                CostingService.IssueResult issueResult = costingService.issue(issueReq);
                subtotal = issueResult.getCostAmount();
                unitCost = issueResult.getCostPrice() == null ? BigDecimal.ZERO : issueResult.getCostPrice();

                CostingService.ReceiptRequest receiptReq = new CostingService.ReceiptRequest();
                receiptReq.setProductId(productId);
                receiptReq.setWarehouseId(toWarehouseId);
                receiptReq.setQty(qty);
                receiptReq.setUnitCost(unitCost);
                receiptReq.setInboundDate(CostingService.toLocalDate(inventoryTransfer.getTransferDate()));
                receiptReq.setOrderId(inventoryTransfer.getId());
                receiptReq.setOrderType(OperationType.调拨入库);
                receiptReq.setItemId(inventoryTransferItem.getId());
                receiptReq.setMerchantId(inventoryTransfer.getMerchantId());
                receiptReq.setAccountBookId(inventoryTransfer.getAccountBookId());
                costingService.createReceiptBatch(receiptReq);
            } else {
                boolean hasConsume = costingService.hasIssueConsumes(inventoryTransfer.getId(), OperationType.调拨出库,
                        inventoryTransferItem.getId(), inventoryTransfer.getMerchantId(), inventoryTransfer.getAccountBookId());
                subtotal = costingService.sumIssueCost(inventoryTransfer.getId(), OperationType.调拨出库,
                        productId, fromWarehouseId, inventoryTransferItem.getId(),
                        inventoryTransfer.getMerchantId(), inventoryTransfer.getAccountBookId());
                if (!hasConsume && fromInventory.getAverageCost() != null) {
                    subtotal = fromInventory.getAverageCost().multiply(BigDecimal.valueOf(qty))
                            .setScale(2, RoundingMode.HALF_EVEN);
                }
            }
            Double qtyForItem = (double) qty;
            this.operateTransferItem(reduceInventory, fromWarehouseId, productId, fromInventory, qtyForItem, subtotal);
            this.operateTransferItem(increaseInventory, toWarehouseId, productId, toInventory, qtyForItem, subtotal);
            InventoryItem toInventoryItem = this.getInventoryItem(inventoryTransferItem, inventoryTransfer, toInventory,
                    qtyForItem, subtotal, toWarehouseId, false);
            inventoryItems.add(toInventoryItem);
            InventoryItem formInventoryItem = this.getInventoryItem(inventoryTransferItem, inventoryTransfer, fromInventory,
                    qtyForItem, subtotal, fromWarehouseId, true);
            inventoryItems.add(formInventoryItem);
        }
        if (isRevoke) {
            increaseInventory.forEach(item ->
                    inventoryService.computedInventory(item, false, inventoryTransfer.getId(), OperationType.调拨入库, null));
            reduceInventory.forEach(item ->
                    inventoryService.computedInventory(item, true, inventoryTransfer.getId(), OperationType.调拨出库, null));
            costingService.reverseIssue(inventoryTransfer.getId(), OperationType.调拨出库,
                    inventoryTransfer.getMerchantId(), inventoryTransfer.getAccountBookId());
            return;
        }
        List<InventoryItem> sortedInventoryItems = new ArrayList<>();
        inventoryItems.forEach(item -> {
            if (item.getWarehouseId().equals(inventoryTransfer.getFromWarehouseId())
                    || Objects.equals(item.getOperationType(), OperationType.调拨出库)) {
                sortedInventoryItems.add(item);
            }
        });
        inventoryItems.forEach(item -> {
            if (Objects.equals(item.getOperationType(), OperationType.调拨入库)) {
                sortedInventoryItems.add(item);
            }
        });
        reduceInventory.forEach(item ->
                inventoryService.computedInventory(item, false, inventoryTransfer.getId(), OperationType.调拨出库, sortedInventoryItems));
        increaseInventory.forEach(item ->
                inventoryService.computedInventory(item, true, inventoryTransfer.getId(), OperationType.调拨入库, sortedInventoryItems));
    }

    /**
     * 获取库存明细列表
     *
     * @param inventoryTransferItem 调拨明细
     * @return inventoryItem 库存明细
     */
    private InventoryItem getInventoryItem(InventoryTransferItem inventoryTransferItem, InventoryTransfer inventoryTransfer,
                                           Inventory inventory, Double transferQuantity, BigDecimal subtotal, Long warehouseId, Boolean isOut) {
        InventoryItem inventoryItem = new InventoryItem();
        inventoryItem.setProductId(inventoryTransferItem.getProductId());
        inventoryItem.setWarehouseId(warehouseId);
        inventoryItem.setInventoryDate(inventoryTransfer.getTransferDate());
        int qty = transferQuantity == null ? 0 : (int) Math.round(transferQuantity);
        inventoryItem.setQuantity(isOut ? -qty : qty);
        inventoryItem.setOperationType(isOut ? OperationType.调拨出库 : OperationType.调拨入库);
        inventoryItem.setBaseUnitId(inventory.getBaseUnitId());
        inventoryItem.setOrderId(inventoryTransfer.getId());
        inventoryItem.setBatchNumber(inventoryTransfer.getOrderNo());
        inventoryItem.setMerchantId(inventoryTransfer.getMerchantId());
        inventoryItem.setAccountBookId(inventoryTransfer.getAccountBookId());
        inventoryItem.setCreatedAt(LocalDateTime.now());
        inventoryItem.setCreatedBy(inventoryTransfer.getCreatedBy());
        inventoryItem.setSubtotal(subtotal);
        if (qty != 0 && subtotal != null) {
            inventoryItem.setUnitPrice(subtotal.divide(BigDecimal.valueOf(qty), 2, RoundingMode.HALF_EVEN));
        } else {
            inventoryItem.setUnitPrice(BigDecimal.ZERO);
        }
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
                    .setScale(2, RoundingMode.HALF_EVEN);
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

    public List<Map<String, Object>> load(Long merchantId, Long id) {
        StringTemplate dateExpressions = Expressions.
                stringTemplate("DATE_FORMAT({0},'%Y-%m-%d')", qInventoryTransfer.transferDate);
        NumberExpression<Integer> numberExpression = new CaseBuilder()
                .when(qInventoryTransferItem.productId.eq(qInventory.productId))
                .then(qInventory.currentQuantity)
                .otherwise(0);
        List<Tuple> fetch = jqf.selectFrom(qInventoryTransfer)
                .select(
                        qInventoryTransfer.id.as("id"),
                        dateExpressions.as("transferDate"),
                        qInventoryTransfer.orderStatus.as("orderStatus"),
                        qInventoryTransfer.FromWarehouseId.as("fromWarehouseId"),
                        qInventoryTransfer.ToWarehouseId.as("toWarehouseId"),
                        formQWarehouse.name.as("fromWarehouseName"),
                        toQWarehouse.name.as("toWarehouseName"),
                        qAdmin.name.as("adminName"),
                        qInventoryTransfer.remarks.as("remarks"),
                        qInventoryTransferItem.id.as("itemId"),
                        qProduct.id.as("productId"),
                        qProduct.imgPath.as("productUrl"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProduct.specification.as("productSpecification"),
                        qProduct.productCategoryId.as("productCategoryId"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.unitId.as("productUnitId"),
                        qUnit.name.as("productUnitName"),
                        qInventoryTransferItem.quantity.as("quantity"),
                        qInventory.currentQuantity.sum().as("warehouseTotal"),
                        numberExpression.sum().as("warehouseQuantity")
                )
                .leftJoin(qInventoryTransferItem).on(qInventoryTransferItem.inventoryTransferId.eq(qInventoryTransfer.id))
                .leftJoin(qProduct).on(qProduct.id.eq(qInventoryTransferItem.productId))
                .leftJoin(qInventory).on(qInventory.warehouseId.eq(qInventoryTransfer.FromWarehouseId))
                .leftJoin(qProductCategory).on(qProductCategory.id.eq(qProduct.productCategoryId))
                .leftJoin(qUnit).on(qUnit.id.eq(qProduct.unitId))
                .leftJoin(qAdmin).on(qAdmin.id.eq(qInventoryTransfer.createdBy))
                .leftJoin(toQWarehouse).on(toQWarehouse.id.eq(qInventoryTransfer.ToWarehouseId))
                .leftJoin(formQWarehouse).on(formQWarehouse.id.eq(qInventoryTransfer.FromWarehouseId))
                .where(qInventoryTransfer.id.eq(id).and(qInventoryTransfer.merchantId.eq(merchantId)))
                .groupBy(qInventoryTransferItem.id).fetch();
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> item;
        for (Tuple tuple : fetch) {
            item = new HashMap<>();
            item.put("id", tuple.get(qInventoryTransfer.id.as("id")));
            item.put("transferDate", tuple.get(dateExpressions.as("transferDate")));
            item.put("orderStatus", tuple.get(qInventoryTransfer.orderStatus.as("orderStatus")));
            item.put("fromWarehouseId", tuple.get(qInventoryTransfer.FromWarehouseId.as("fromWarehouseId")));
            item.put("toWarehouseId", tuple.get(qInventoryTransfer.ToWarehouseId.as("toWarehouseId")));
            item.put("fromWarehouseName", tuple.get(formQWarehouse.name.as("fromWarehouseName")));
            item.put("toWarehouseName", tuple.get(toQWarehouse.name.as("toWarehouseName")));
            item.put("adminName", tuple.get(qAdmin.name.as("adminName")));
            item.put("remarks", tuple.get(qInventoryTransfer.remarks.as("remarks")));
            item.put("itemId", tuple.get(qInventoryTransferItem.id.as("itemId")));
            item.put("productId", tuple.get(qProduct.id.as("productId")));
            item.put("productUrl", tuple.get(qProduct.imgPath.as("productUrl")));
            item.put("productCode", tuple.get(qProduct.code.as("productCode")));
            item.put("productName", tuple.get(qProduct.name.as("productName")));
            item.put("productSpecification", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productCategoryId", tuple.get(qProductCategory.name.as("productCategoryName")));
            item.put("productCategoryName", tuple.get(qProduct.specification.as("productSpecification")));
            item.put("productUnitId", tuple.get(qProduct.unitId.as("productUnitId")));
            item.put("productUnitName", tuple.get(qUnit.name.as("productUnitName")));
            item.put("quantity", tuple.get(qInventoryTransferItem.quantity.as("quantity")));
            item.put("warehouseTotal", tuple.get(qInventory.currentQuantity.sum().as("warehouseTotal")));
            item.put("warehouseQuantity", tuple.get(numberExpression.sum().as("warehouseQuantity")));
            result.add(item);
        }
        return result;
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private OrderStatus state;

        private String filter;

        private String productIds;

        private String fromWarehouseIds;

        private String toWarehouseIds;

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
            if (StrUtil.isNotBlank(productIds)) {
                builder.and(formQWarehouse.id.isNotNull()).
                        and(toQWarehouse.id.isNotNull()).
                        and(qAdmin.id.isNotNull()).
                        and(qInventoryTransferItem.productId.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(fromWarehouseIds)) {
                builder.and(qInventoryTransfer.FromWarehouseId.in(Arrays.stream(fromWarehouseIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(toWarehouseIds)) {
                builder.and(qInventoryTransfer.ToWarehouseId.in(Arrays.stream(toWarehouseIds.split(",")).map(Long::parseLong).toList()));
            }
            return builder;
        }

    }
}
