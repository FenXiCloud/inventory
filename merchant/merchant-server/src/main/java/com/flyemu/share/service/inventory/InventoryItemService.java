package com.flyemu.share.service.inventory;

import cn.dev33.satoken.exception.InvalidContextException;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.InventoryItemBalanceDTO;
import com.flyemu.share.dto.InventoryItemDTO;
import com.flyemu.share.dto.InventoryItemReportDto;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.inventory.QInventory;
import com.flyemu.share.entity.inventory.QInventoryItem;
import com.flyemu.share.entity.setting.FinanceVoucher;
import com.flyemu.share.entity.setting.QFinanceVoucher;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.form.InventoryInitialForm;
import com.flyemu.share.repository.InventoryItemRepository;
import com.flyemu.share.repository.ProductRepository;
import com.flyemu.share.repository.WarehouseRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.criteria.Predicate;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;

/**
 * @功能描述: 库存明细交易流水
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InventoryItemService extends AbsService {

    private final static QInventoryItem qInventoryItem = QInventoryItem.inventoryItem;

    private final static QProduct qProduct = QProduct.product;

    private final static QProductCategory qProductCategory = QProductCategory.productCategory;

    private final static QWarehouse qWarehouse = QWarehouse.warehouse;

    private final static QUnit qUnit = QUnit.unit;

    private final static QSupplier qSupplier = QSupplier.supplier;

    private final static QInventory qInventory = QInventory.inventory;

    private final static QCustomer qCustomer = QCustomer.customer;

    private final static QFinanceVoucher qFinanceVoucher = QFinanceVoucher.financeVoucher;

    private final InventoryItemRepository inventoryItemRepository;

    private final ProductRepository productRepository;

    private final WarehouseRepository warehouseRepository;

    public List<InventoryItem> query(Query query) {
        List<InventoryItem> inventoryItems = bqf.selectFrom(qInventoryItem)
                .where(query.builder)
                .orderBy(qInventoryItem.id.desc())
                .fetch();
        return inventoryItems;
    }

    public PageResults<InventoryItemDTO> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qInventoryItem)
                .select(qInventoryItem, qProduct.name, qProduct.code, qProduct.specification, qWarehouse.name, qUnit.name)
                .leftJoin(qProduct).on(qProduct.id.eq(qInventoryItem.productId))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventoryItem.warehouseId))
                .leftJoin(qUnit).on(qUnit.id.eq(qInventoryItem.baseUnitId))
                .where(query.buildersV2())
                .orderBy(qInventoryItem.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());

        ArrayList<InventoryItemDTO> collect = fetchPage.stream().collect(ArrayList::new, (list, tuple) -> {
            InventoryItemDTO dto = BeanUtil.toBean(tuple.get(qInventoryItem), InventoryItemDTO.class);
            dto.setProductName(tuple.get(qProduct.name));
            dto.setProductCode(tuple.get(qProduct.code));
            dto.setSpecification(tuple.get(qProduct.specification));
            dto.setWarehouseName(tuple.get(qWarehouse.name));
            dto.setUnitName(tuple.get(qUnit.name));
            list.add(dto);
        }, List::addAll);

        return new PageResults<>(collect, page, fetchPage.getTotalSize());
    }

    @Transactional
    public InventoryItem save(InventoryItem inventoryItem) {
        inventoryItem.setFirstSort(false);
        if (inventoryItem.getId() != null) {
            //更新
            InventoryItem original = inventoryItemRepository.getById(inventoryItem.getId());
            BeanUtil.copyProperties(inventoryItem, original, CopyOptions.create().ignoreNullValue());
            return inventoryItemRepository.save(original);
        }
        return inventoryItemRepository.save(inventoryItem);
    }

    @Transactional
    public void delete(Long inventoryItemId, Long merchantId, Long accountBookId) {
        jqf.delete(qInventoryItem)
                .where(qInventoryItem.id.eq(inventoryItemId).and(qInventoryItem.merchantId.eq(merchantId)).and(qInventoryItem.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<InventoryItem> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qInventoryItem).where(qInventoryItem.merchantId.eq(merchantId).and(qInventoryItem.accountBookId.eq(accountBookId))).fetch();
    }

    /**
     * 根据订单号获取库存明细
     *
     * @param orderId 订单id
     * @return list
     */
    public List<InventoryItem> findByOrderId(Long orderId) {
        return jqf.selectFrom(qInventoryItem).where(qInventoryItem.orderId.eq(orderId)).fetch();
    }

    /**
     * 根据订单号删除库存明细
     *
     * @param orderId 订单id
     */
    public void deleteByOrderId(Long orderId, OperationType operationType) {
        jqf.delete(qInventoryItem).where(qInventoryItem.orderId.eq(orderId).and(qInventoryItem.operationType.eq(operationType))).execute();
    }


    /**
     * 批量插入
     *
     * @param list 列表
     */
    public void batchInsertList(List<InventoryItem> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // 明细时间记录为录入时间（排序用）
        list.forEach(item -> {
            item.setCreatedAt(LocalDateTime.now());
        });
        inventoryItemRepository.saveAll(list);
    }

    public PageResults<InventoryItemReportDto> report(Page page, Query query) {
        List<Long> ids = this.findPreviousMonthIds(query);
        JPAQuery<Tuple> tupleJPAQuery = jqf.selectFrom(qInventoryItem)
                .select(
                        qInventoryItem.id,
                        qInventoryItem.firstSort,
                        qInventoryItem.warehouseId,
                        qInventoryItem.batchNumber,
                        qProduct.id.as("productId"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProduct.productCategoryId.as("productCategoryId"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.specification.as("productSpecification"),
                        qInventoryItem.createdAt.as("createdAt"),
                        qInventoryItem.quantity.as("quantity"),
                        qInventoryItem.orderId.as("orderId"),
                        qInventoryItem.operationType.as("operationType"),
                        qWarehouse.name.as("warehouseName"),
                        qProduct.remarks.as("productRemarks"),
                        qUnit.name.as("unitName"),
                        qSupplier.id.as("supplierId"),
                        qSupplier.name.as("supplierName"),
                        qCustomer.id.as("customerId"),
                        qCustomer.name.as("customerName"),
                        qInventoryItem.unitPrice.as("unitPrice"),
                        qInventoryItem.subtotal.as("subtotal"),
                        qInventoryItem.currentQuantity.as("currentQuantity"),
                        qInventoryItem.totalCost.as("totalCost"),
                        qInventoryItem.averageCost.as("averageCost"),
                        qFinanceVoucher.voucherId.as("voucherId"),
                        qFinanceVoucher.code.as("voucherCode")
                )
                .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventoryItem.warehouseId))
                .leftJoin(qUnit).on(qInventoryItem.baseUnitId.eq(qUnit.id))
                .leftJoin(qSupplier).on(qInventoryItem.supplierId.eq(qSupplier.id))
                .leftJoin(qCustomer).on(qInventoryItem.customerId.eq(qCustomer.id))
                .leftJoin(qFinanceVoucher).on(qFinanceVoucher.orderId.eq(qInventoryItem.id))
                .where(query.builders())
                .where(qProduct.id.isNotNull())
                .where(qInventoryItem.operationType.ne(OperationType.期初余额).or(qInventoryItem.id.in(ids)))
                .orderBy(qInventoryItem.productId.asc())
                .orderBy(qInventoryItem.firstSort.desc())
                .orderBy(qInventoryItem.createdAt.desc())
                .orderBy(qInventoryItem.id.asc());
        long size = tupleJPAQuery.fetchCount();
        List<Tuple> fetch = tupleJPAQuery
                .offset(page.getOffset())
                .limit(page.getOffsetEnd())
                .fetch();
        List<InventoryItemReportDto> dtos = new ArrayList<>();
        InventoryItemReportDto dto;
        for (Tuple tuple : fetch) {
            dto = new InventoryItemReportDto();
            dto.setId(tuple.get(qInventoryItem.id));
            dto.setBatchNumber(tuple.get(qInventoryItem.batchNumber));
            dto.setProductId(tuple.get(qProduct.id.as("productId")));
            dto.setProductCategoryId(tuple.get(qProduct.id.as("productCategoryId")));
            dto.setProductCode(tuple.get(qProduct.code.as("productCode")));
            dto.setProductName(tuple.get(qProduct.name.as("productName")));
            dto.setSupplierId(tuple.get(qSupplier.id.as("supplierId")));
            dto.setSupplierName(tuple.get(qSupplier.name.as("supplierName")));
            dto.setCustomerId(tuple.get(qCustomer.id.as("customerId")));
            dto.setCustomerName(tuple.get(qCustomer.name.as("customerName")));
            dto.setCreatedAt(tuple.get(qInventoryItem.createdAt.as("createdAt")));
            dto.setProductCategoryName(tuple.get(qProductCategory.name.as("productCategoryName")));
            dto.setProductSpecification(tuple.get(qProduct.specification.as("productSpecification")));
            dto.setUnitName(tuple.get(qUnit.name.as("unitName")));
            dto.setProductRemarks(tuple.get(qProduct.remarks.as("productRemarks")));
            dto.setOperationType(tuple.get(qInventoryItem.operationType.as("operationType")));
            dto.setWarehouseName(tuple.get(qWarehouse.name.as("warehouseName")));
            dto.setUnitPrice(tuple.get(qInventoryItem.unitPrice.as("unitPrice")));
            dto.setSubtotal(tuple.get(qInventoryItem.subtotal.as("subtotal")));
            dto.setTotalCost(tuple.get(qInventoryItem.totalCost.as("totalCost")));
            dto.setAverageCost(tuple.get(qInventoryItem.averageCost.as("averageCost")));
            dto.setCurrentQuantity(tuple.get(qInventoryItem.currentQuantity.as("currentQuantity")));
            dto.setQuantity(tuple.get(qInventoryItem.quantity.as("quantity")));
            dto.setVoucherId(tuple.get(qFinanceVoucher.voucherId.as("voucherId")));
            dto.setVoucherCode(tuple.get(qFinanceVoucher.code.as("voucherCode")));
            dtos.add(dto);
        }
        return new PageResults<>(dtos, page, size);
    }

    private List<Long> findPreviousMonthIds(Query query) {
        Date start = query.getStart();
        if (start == null) {
            start = new Date();
        }
        // 获取上个月期初余额明细数据
        LocalDate firstDayDate = start.toInstant().atZone(ZoneId.systemDefault()).toLocalDate().with(TemporalAdjusters.firstDayOfMonth());
        // 创建一个DateTimeFormatter对象
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        // 使用formatter格式化LocalDate对象
        String formattedDate = firstDayDate.minusMonths(1).format(formatter);
        return inventoryItemRepository.findInventoryItemQcByTime(formattedDate);
    }

    private static Date addTimeOfFinalMoment(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR, 23);
        calendar.add(Calendar.MINUTE, 59);
        calendar.add(Calendar.SECOND, 59);
        return calendar.getTime();
    }

    public PageResults<InventoryItemReportDto> summary(Page page, Query query) {
        Boolean exclusion = query.getExclusion();
        List<Long> voucherOrderIds = new ArrayList<>();
        if (Boolean.TRUE.equals(exclusion)) {
            List<FinanceVoucher> fetch = jqf.selectFrom(qFinanceVoucher).fetch();
            for (FinanceVoucher financeVoucher : fetch) {
                voucherOrderIds.add(financeVoucher.getOrderId());
            }
        }
        PagedList<Tuple> fetchPage = bqf.selectFrom(qInventoryItem)
                .select(
                        qProduct.id.as("productId"),
                        qProduct.code.as("productCode"),
                        qProduct.name.as("productName"),
                        qProduct.productCategoryId.as("productCategoryId"),
                        qProductCategory.name.as("productCategoryName"),
                        qProduct.specification.as("productSpecification"),
                        qWarehouse.name.as("warehouseName"),
                        qWarehouse.id.as("warehouseId"),
                        qProduct.remarks.as("productRemarks"),
                        qUnit.name.as("unitName")
                )
                .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventoryItem.warehouseId))
                .leftJoin(qUnit).on(qInventoryItem.baseUnitId.eq(qUnit.id))
                .where(query.builders())
                .where(qInventoryItem.id.notIn(voucherOrderIds))
                .where(qProduct.id.isNotNull())
                .where(qInventoryItem.operationType.ne(OperationType.期初余额))
                .groupBy(qProduct.id)
                .orderBy(qProduct.id.asc())
                .orderBy(qWarehouse.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        List<InventoryItemReportDto> dtos = new ArrayList<>();
        InventoryItemReportDto dto;
        for (Tuple tuple : fetchPage) {
            dto = new InventoryItemReportDto();
            dto.setProductId(tuple.get(qProduct.id.as("productId")));
            dto.setProductCategoryId(tuple.get(qProduct.id.as("productCategoryId")));
            dto.setProductCode(tuple.get(qProduct.code.as("productCode")));
            dto.setProductName(tuple.get(qProduct.name.as("productName")));
            dto.setProductCategoryName(tuple.get(qProductCategory.name.as("productCategoryName")));
            dto.setProductSpecification(tuple.get(qProduct.specification.as("productSpecification")));
            dto.setProductRemarks(tuple.get(qProduct.remarks.as("productRemarks")));
            dto.setOperationType(tuple.get(qInventoryItem.operationType.as("operationType")));
            dto.setUnitName(tuple.get(qUnit.name.as("unitName")));
            dto.setWarehouseName(tuple.get(qWarehouse.name.as("warehouseName")));
            dto.setWarehouseId(tuple.get(qWarehouse.id.as("warehouseId")));
            if (dto.getProductId() != null && dto.getWarehouseId() != null) {
                Inventory inventory = jqf.selectFrom(qInventory).where(qInventory.productId.eq(dto.getProductId()).and(qInventory.warehouseId.eq(dto.getWarehouseId()))).fetchOne();
                if (inventory != null) {
                    dto.setTotalCost(inventory.getTotalCost());
                    dto.setCurrentQuantity(inventory.getCurrentQuantity());
                } else {
                    dto.setTotalCost(BigDecimal.ZERO);
                    dto.setCurrentQuantity(0);
                }
            }
            dtos.add(dto);
        }
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public List<InventoryItemReportDto> summaryOperationType(Query query) {
        List<Tuple> fetchPage = bqf.selectFrom(qInventoryItem)
                .select(
                        qProduct.id.as("productId"),
                        qWarehouse.id.as("warehouseId"),
                        qInventoryItem.operationType.as("operationType"),
                        qInventoryItem.quantity.sum().as("quantity"),
                        qInventoryItem.subtotal.sum().as("subtotal")
                )
                .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qWarehouse).on(qWarehouse.id.eq(qInventoryItem.warehouseId))
                .leftJoin(qUnit).on(qInventoryItem.baseUnitId.eq(qUnit.id))
                .where(query.builders())
                .where(qInventoryItem.operationType.ne(OperationType.期初余额))
                .groupBy(qInventoryItem.operationType, qProduct.id, qWarehouse.id)
                .orderBy(qProduct.id.asc())
                .fetch();
        List<InventoryItemReportDto> dtos = new ArrayList<>();
        InventoryItemReportDto dto;
        for (Tuple tuple : fetchPage) {
            dto = new InventoryItemReportDto();
            dto.setProductId(tuple.get(qProduct.id.as("productId")));
            dto.setWarehouseId(tuple.get(qWarehouse.id.as("warehouseId")));
            dto.setOperationType(tuple.get(qInventoryItem.operationType.as("operationType")));
            dto.setQuantity(tuple.get(qInventoryItem.quantity.sum().as("quantity")));
            dto.setSubtotal(tuple.get(qInventoryItem.subtotal.sum().as("subtotal")));
            dtos.add(dto);
        }
        return dtos;
    }

    public List<InventoryItemReportDto> summaryInitial(Query query) {
        List<Long> ids = this.findPreviousMonthIds(query);
        List<Tuple> fetchPage = bqf.selectFrom(qInventoryItem)
                .select(
                        qProduct.id.as("productId"),
                        qInventoryItem.operationType.as("operationType"),
                        qInventoryItem.currentQuantity.as("quantity"),
                        qInventoryItem.totalCost.as("subtotal")
                )
                .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qUnit).on(qInventoryItem.baseUnitId.eq(qUnit.id))
                .where(qInventoryItem.operationType.eq(OperationType.期初余额).and(qInventoryItem.id.in(ids)))
                .groupBy(qInventoryItem.operationType, qProduct.id)
                .orderBy(qProduct.id.asc())
                .fetch();
        List<InventoryItemReportDto> dtos = new ArrayList<>();
        InventoryItemReportDto dto;
        for (Tuple tuple : fetchPage) {
            dto = new InventoryItemReportDto();
            dto.setProductId(tuple.get(qProduct.id.as("productId")));
            dto.setOperationType(tuple.get(qInventoryItem.operationType.as("operationType")));
            dto.setQuantity(tuple.get(qInventoryItem.currentQuantity.as("quantity")));
            dto.setSubtotal(tuple.get(qInventoryItem.totalCost.as("subtotal")));
            dtos.add(dto);
        }
        return dtos;
    }

    @Transactional
    public void batchSave(InventoryInitialForm inventoryInitialForm) {
        List<InventoryItem> inventoryItemList = inventoryInitialForm.getInventoryItemList();
        for (InventoryItem inventoryItem : inventoryItemList) {
            inventoryItem.setAccountBookId(inventoryInitialForm.getAccountBookId());
            inventoryItem.setMerchantId(inventoryInitialForm.getMerchantId());
            inventoryItem.setCreatedBy(inventoryInitialForm.getCreatedBy());
            inventoryItem.setCreatedAt(LocalDateTime.now());
            if (inventoryItem.getId() != null) {
                //更新
                InventoryItem original = inventoryItemRepository.getById(inventoryItem.getId());
                BeanUtil.copyProperties(inventoryItem, original, CopyOptions.create().ignoreNullValue());
                inventoryItemRepository.save(original);
            } else {
                //按照商品id和仓库id 查询数据是否存在，组装查询条件
                Specification<InventoryItem> query = (root, criteriaQuery, criteriaBuilder) -> {
                    List<Predicate> predicates = new ArrayList<>();
                    predicates.add(criteriaBuilder.equal(root.get("productId"), inventoryItem.getProductId()));
                    predicates.add(criteriaBuilder.equal(root.get("warehouseId"), inventoryItem.getWarehouseId()));
                    predicates.add(criteriaBuilder.equal(root.get("accountBookId"), inventoryItem.getAccountBookId()));
                    predicates.add(criteriaBuilder.equal(root.get("merchantId"), inventoryItem.getMerchantId()));
                    predicates.add(criteriaBuilder.equal(root.get("operationType"), OperationType.期初库存));
                    return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
                };
                //如果存在，则抛出异常
                if (inventoryItemRepository.exists(query)) {
                    //根据商品id查询商品
                    Product product = productRepository.getById(inventoryItem.getProductId());
                    //根据仓库id查询仓库
                    Warehouse warehouse = warehouseRepository.getById(inventoryItem.getWarehouseId());
                    throw new InvalidContextException("商品：" + product.getName() + "，仓库：" + warehouse.getName() + "，期初库存数据已存在");
                }
                inventoryItem.setUpdatedAt(LocalDateTime.now());
                inventoryItem.setFirstSort(false);
                //新增
                inventoryItemRepository.save(inventoryItem);
            }
        }
    }

    public InventoryItemDTO getById(InventoryItem query) {
        InventoryItem inventoryItem = inventoryItemRepository.getById(query.getId());
        InventoryItemDTO dto = BeanUtil.toBean(inventoryItem, InventoryItemDTO.class);
        return dto;
    }

    @Transactional
    public void batchDelete(InventoryInitialForm inventoryInitialForm) {
        List<Long> ids = inventoryInitialForm.getIds();
        if (ids.isEmpty()) {
            return;
        }
        inventoryItemRepository.deleteAllByIdInBatch(ids);
    }

    public PageResults<InventoryItemBalanceDTO> balance(Page page, Query query) {
        BooleanExpression expression = qInventoryItem.createdAt.in(
                JPAExpressions.select(qInventoryItem.createdAt.max().as("createdAt")).from(qInventoryItem)
                        .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                        .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                        .leftJoin(qWarehouse).on(qInventoryItem.warehouseId.eq(qWarehouse.id))
                        .where(qInventoryItem.operationType.notIn(OperationType.期初余额, OperationType.期初库存))
                        .where(qProduct.id.isNotNull(), qWarehouse.id.isNotNull())
                        .orderBy(qInventoryItem.createdAt.asc())
                        .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId)
        ).and(qInventoryItem.operationType.notIn(OperationType.期初余额, OperationType.期初库存));
        JPAQuery<Tuple> timerJpaQuery = getBalanceJpaQuery(query, expression);
        JPAQuery<Tuple> tupleJPAQuery = getBalanceJpaQuery(query, expression);
        // 获取日期范围内数据
        BooleanBuilder timerBuilder = new BooleanBuilder();
        Date start = query.getStart();
        Date end = query.getEnd();
        if (start != null) {
            timerBuilder.and(qInventoryItem.createdAt.loe(LocalDateTime.ofInstant(addTimeOfFinalMoment(start).toInstant(), ZoneId.systemDefault())));
            timerBuilder.and(qInventoryItem.createdAt.goe(LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())));
        }
        timerJpaQuery.where(timerBuilder)
                .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId);
        List<Tuple> fetch = timerJpaQuery.fetch();
        // 获取日期开始时间前数据
        List<Tuple> beforeFetch = new ArrayList<>();
        if (start != null && end != null) {
            BooleanBuilder whereBuilder = new BooleanBuilder();
            whereBuilder.and(qInventoryItem.createdAt.loe(LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())));
            whereBuilder.and(qInventoryItem.productId.notIn(
                    JPAExpressions.select(qInventoryItem.productId).from(qInventoryItem)
                            .where(expression)
                            .where(timerBuilder)
                            .where(query.balanceBuilders())
            ).and(qInventoryItem.warehouseId.notIn(
                    JPAExpressions.select(qInventoryItem.warehouseId).from(qInventoryItem)
                            .where(expression)
                            .where(timerBuilder)
                            .where(query.balanceBuilders())
            )));
            beforeFetch = tupleJPAQuery.where(whereBuilder).groupBy(qInventoryItem.productId, qInventoryItem.warehouseId).fetch();
        }
        fetch.addAll(beforeFetch);
        List<InventoryItemBalanceDTO> dtos = new ArrayList<>();
        for (Tuple tuple : fetch) {
            InventoryItemBalanceDTO dto = new InventoryItemBalanceDTO();
            dto.setProductId(tuple.get(qInventoryItem.productId.as("productId")));
            dto.setInventoryItemId(tuple.get(qInventoryItem.id.as("inventoryItemId")));
            dto.setCreatedAt(tuple.get(qInventoryItem.createdAt.as("createdAt")));
            dto.setWarehouseId(tuple.get(qInventoryItem.warehouseId.as("warehouseId")));
            dto.setTotalCost(tuple.get(qInventoryItem.totalCost.as("totalCost")));
            dto.setCurrentQuantity(tuple.get(qInventoryItem.currentQuantity.as("currentQuantity")));
            dto.setAverageCost(tuple.get(qInventoryItem.averageCost.as("averageCost")));
            dto.setWarehouseName(tuple.get(qWarehouse.name.as("warehouseName")));
            dtos.add(dto);
        }
        return new PageResults<>(dtos, page, fetch.size());
    }

    private JPAQuery<Tuple> getBalanceJpaQuery(Query query, BooleanExpression expression) {
        return jqf.selectFrom(qInventoryItem)
                .select(
                        qInventoryItem.id.as("inventoryItemId"),
                        qInventoryItem.productId.as("productId"),
                        qInventoryItem.createdAt.as("createdAt"),
                        qInventoryItem.warehouseId.as("warehouseId"),
                        qWarehouse.name.as("warehouseName"),
                        qInventoryItem.totalCost.as("totalCost"),
                        qInventoryItem.currentQuantity.as("currentQuantity"),
                        qInventoryItem.averageCost.as("averageCost")
                )
                .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                .leftJoin(qWarehouse).on(qInventoryItem.warehouseId.eq(qWarehouse.id))
                .where(query.balanceBuilders())
                .where(expression)
                .where(qProduct.id.isNotNull(), qWarehouse.id.isNotNull())
                .orderBy(qInventoryItem.createdAt.asc());
    }

    @Data
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        private Date start;

        private Date end;

        private String filter;

        private String summaryFilter;

        private Long warehouseId;

        private String warehouseIds;

        private Long supplierId;

        private String supplierIds;

        private String customerIds;

        private Long productId;

        private String productIds;

        private String productCategoryIds;

        @Enumerated(EnumType.STRING)
        private OperationType operationType;

        private String operationTypes;

        private Boolean exclusion;

        private Boolean isReport;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qInventoryItem.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventoryItem.accountBookId.eq(accountBookId));
            }
        }

        public BooleanBuilder builders() {
            if (start != null && end != null && (isReport == null || Boolean.FALSE.equals(isReport))) {
                builder.and(qInventoryItem.createdAt.loe(LocalDateTime.ofInstant(addTimeOfFinalMoment(end).toInstant(), ZoneId.systemDefault())));
                builder.and(qInventoryItem.createdAt.goe(LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())));
            }
            if (start != null && end != null && Boolean.TRUE.equals(isReport)) {
                builder.and(
                        qInventoryItem.createdAt.loe(LocalDateTime.ofInstant(addTimeOfFinalMoment(end).toInstant(), ZoneId.systemDefault()))
                                .and(qInventoryItem.createdAt.goe(LocalDateTime.ofInstant(start.toInstant(), ZoneId.systemDefault())))
                                .or(qInventoryItem.operationType.eq(OperationType.期初余额))
                );
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qInventoryItem.batchNumber.contains(filter))
                        .or(qProduct.name.contains(filter));
            }
            if (StrUtil.isNotBlank(summaryFilter) && StrUtil.isNotBlank(summaryFilter.trim())) {
                builder.and(qProduct.code.contains(summaryFilter))
                        .or(qProduct.name.contains(summaryFilter));
            }
            if (warehouseId != null) {
                builder.and(qInventoryItem.warehouseId.eq(warehouseId));
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                builder.and(qInventoryItem.warehouseId.in(Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList()));
            }
            if (supplierId != null) {
                builder.and(qInventoryItem.supplierId.eq(supplierId));
            }
            if (StrUtil.isNotBlank(supplierIds)) {
                builder.and(qInventoryItem.supplierId.in(Arrays.stream(supplierIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(customerIds)) {
                builder.and(qInventoryItem.customerId.in(Arrays.stream(customerIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(productCategoryIds)) {
                builder.and(qProduct.productCategoryId.in(Arrays.stream(productCategoryIds.split(",")).map(Long::parseLong).toList()));
            }
            if (productId != null) {
                builder.and(qInventoryItem.productId.eq(productId));
            }
            if (StrUtil.isNotBlank(productIds)) {
                builder.and(qInventoryItem.productId.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).toList()));
            }
            if (operationType != null) {
                builder.and(qInventoryItem.operationType.eq(operationType));
            }
            if (StrUtil.isNotBlank(operationTypes)) {
                builder.and(qInventoryItem.operationType.in(Arrays.stream(operationTypes.split(",")).map(OperationType::valueOf).toList()));
            }
            return builder;
        }

        public BooleanBuilder buildersV2() {

            if (operationType != null) {
                builder.and(qInventoryItem.operationType.eq(operationType));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                builder.and(qProduct.code.contains(filter).or(qProduct.name.contains(filter)));
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                builder.and(qInventoryItem.warehouseId.in(Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(productIds)) {
                builder.and(qInventoryItem.productId.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).toList()));
            }
            return builder;
        }

        public BooleanBuilder balanceBuilders() {
            if (StrUtil.isNotBlank(productIds)) {
                builder.and(qProduct.id.in(Arrays.stream(productIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(productCategoryIds)) {
                builder.and(qProduct.productCategoryId.in(Arrays.stream(productCategoryIds.split(",")).map(Long::parseLong).toList()));
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                builder.and(qWarehouse.id.in(Arrays.stream(warehouseIds.split(",")).map(Long::parseLong).toList()));
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
