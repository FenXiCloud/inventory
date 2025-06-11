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
import java.math.RoundingMode;
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
            if (original.getInventoryDate() == null) {
                original.setInventoryDate(new Date());
            }
            return inventoryItemRepository.save(original);
        }
        if (inventoryItem.getInventoryDate() == null) {
            inventoryItem.setInventoryDate(new Date());
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
    @Transactional
    public void deleteByOrderId(Long orderId, OperationType operationType) {
        List<InventoryItem> fetch = jqf.selectFrom(qInventoryItem).where(qInventoryItem.orderId.eq(orderId).and(qInventoryItem.operationType.eq(operationType))).fetch();
        inventoryItemRepository.deleteAll(fetch);
        this.sortedInventoryItem(fetch);
        this.sortedSummaryInventoryItem(fetch);
    }

    private ArrayList<OperationType> getIncreaseTypeList() {
        return new ArrayList<>() {{
            add(OperationType.其他入库);
            add(OperationType.盘盈入库);
            add(OperationType.调拨入库);
            add(OperationType.采购入库);
        }};
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
        List<InventoryItem> inventoryItems = inventoryItemRepository.saveAll(list);
        this.sortedInventoryItem(inventoryItems);
        this.sortedSummaryInventoryItem(inventoryItems);
    }

    private void sortedInventoryItem(List<InventoryItem> inventoryItems) {
        List<Map<String, Long>> sorted = new ArrayList<>();
        Date inventoryDate = null;
        for (InventoryItem inventoryItem : inventoryItems) {
            inventoryDate = inventoryItem.getInventoryDate();
            Long productId = inventoryItem.getProductId();
            Long warehouseId = inventoryItem.getWarehouseId();
            Long id = inventoryItem.getId();
            sorted.add(Map.of("productId", productId, "warehouseId", warehouseId, "id", id));
        }
        List<OperationType> increaseTypeList = getIncreaseTypeList();
        Date finalInventoryDate = inventoryDate;
        sorted.forEach(item -> {
            Long productId = item.get("productId");
            Long warehouseId = item.get("warehouseId");
            Long id = item.get("id");
            // 获取对应数据进行排序
            List<InventoryItem> goeFetch = jqf.selectFrom(qInventoryItem).where(
                    qInventoryItem.productId.eq(productId)
                            .and(qInventoryItem.warehouseId.eq(warehouseId))
                            .and(qInventoryItem.inventoryDate.goe(finalInventoryDate))
                            .and(qInventoryItem.operationType.notIn(OperationType.期初库存, OperationType.期初余额))
            ).orderBy(qInventoryItem.inventoryDate.asc()).fetch();
            List<InventoryItem> loeFetch = jqf.selectFrom(qInventoryItem).where(
                    qInventoryItem.productId.eq(productId)
                            .and(qInventoryItem.warehouseId.eq(warehouseId))
                            .and(qInventoryItem.inventoryDate.loe(finalInventoryDate))
                            .and(qInventoryItem.id.ne(id))
                            .and(qInventoryItem.operationType.notIn(OperationType.期初库存, OperationType.期初余额))
            ).orderBy(qInventoryItem.inventoryDate.desc()).fetch();
            Integer currentQuantity = 0;
            BigDecimal totalCost = BigDecimal.ZERO;
            if (!loeFetch.isEmpty()) {
                currentQuantity = loeFetch.get(0).getCurrentQuantity();
                totalCost = loeFetch.get(0).getTotalCost();
            }
            if (currentQuantity == null) {
                currentQuantity = 0;
            }
            if (totalCost == null) {
                totalCost = BigDecimal.ZERO;
            }
            for (InventoryItem inventoryItem : goeFetch) {
                boolean contains = increaseTypeList.contains(inventoryItem.getOperationType());
                if (contains) {
                    currentQuantity = currentQuantity + inventoryItem.getQuantity();
                    totalCost = totalCost.add(inventoryItem.getSubtotal());
                } else {
                    currentQuantity = currentQuantity - inventoryItem.getQuantity();
                    totalCost = totalCost.subtract(inventoryItem.getSubtotal());
                }
                inventoryItem.setCurrentQuantity(currentQuantity);
                inventoryItem.setTotalCost(totalCost);
                if (!OperationType.成本调整.equals(inventoryItem.getOperationType())) {
                    if (currentQuantity == 0) {
                        inventoryItem.setAverageCost(BigDecimal.ZERO);
                    } else {
                        BigDecimal averageCost = totalCost.divide(new BigDecimal(currentQuantity), 2, RoundingMode.HALF_UP);
                        inventoryItem.setAverageCost(averageCost);
                    }
                }
                inventoryItemRepository.save(inventoryItem);
            }
        });
    }

    private void sortedSummaryInventoryItem(List<InventoryItem> inventoryItems) {
        List<Long> sorted = new ArrayList<>();
        Date inventoryDate = null;
        for (InventoryItem inventoryItem : inventoryItems) {
            inventoryDate = inventoryItem.getInventoryDate();
            Long productId = inventoryItem.getProductId();
            if (sorted.contains(productId)) {
                continue;
            }
            sorted.add(productId);
        }
        List<OperationType> increaseTypeList = getIncreaseTypeList();
        Date finalInventoryDate = inventoryDate;
        sorted.forEach(productId -> {
            // 获取对应数据进行排序
            List<InventoryItem> goeFetch = jqf.selectFrom(qInventoryItem).where(
                    qInventoryItem.productId.eq(productId)
                            .and(qInventoryItem.inventoryDate.goe(finalInventoryDate))
                            .and(qInventoryItem.operationType.notIn(OperationType.期初库存, OperationType.期初余额))
            ).orderBy(qInventoryItem.inventoryDate.asc(), qInventoryItem.id.asc()).fetch();
            List<InventoryItem> loeFetch = jqf.selectFrom(qInventoryItem).where(
                    qInventoryItem.productId.eq(productId)
                            .and(qInventoryItem.inventoryDate.lt(finalInventoryDate))
                            .and(qInventoryItem.operationType.notIn(OperationType.期初库存, OperationType.期初余额))
            ).orderBy(qInventoryItem.inventoryDate.desc(), qInventoryItem.id.desc()).fetch();
            Integer summaryQuantity = 0;
            BigDecimal summaryCost = BigDecimal.ZERO;
            if (!loeFetch.isEmpty()) {
                summaryQuantity = loeFetch.get(0).getCurrentQuantity();
                summaryCost = loeFetch.get(0).getTotalCost();
            }
            if (summaryQuantity == null) {
                summaryQuantity = 0;
            }
            if (summaryCost == null) {
                summaryCost = BigDecimal.ZERO;
            }
            for (InventoryItem inventoryItem : goeFetch) {
                boolean contains = increaseTypeList.contains(inventoryItem.getOperationType());
                if (contains) {
                    summaryQuantity = summaryQuantity + inventoryItem.getQuantity();
                    summaryCost = summaryCost.add(inventoryItem.getSubtotal());
                } else {
                    summaryQuantity = summaryQuantity - inventoryItem.getQuantity();
                    summaryCost = summaryCost.subtract(inventoryItem.getSubtotal());
                }
                inventoryItem.setSummaryQuantity(summaryQuantity);
                inventoryItem.setSummaryCost(summaryCost);
                if (!OperationType.成本调整.equals(inventoryItem.getOperationType())) {
                    if (summaryQuantity == 0) {
                        inventoryItem.setSummaryAverage(BigDecimal.ZERO);
                    } else {
                        BigDecimal averageCost = summaryCost.divide(new BigDecimal(summaryQuantity), 2, RoundingMode.HALF_UP);
                        inventoryItem.setSummaryAverage(averageCost);
                    }
                } else {
                    inventoryItem.setSummaryAverage(inventoryItem.getAverageCost());
                }
                inventoryItemRepository.save(inventoryItem);
            }
        });
    }

    public PageResults<InventoryItemReportDto> report(Page page, Query query) {
        Map<String, Object> map = query.toMap();
        Date start = query.getStart();
        map.put("initDate", Objects.requireNonNullElseGet(start, Date::new));
        org.sagacity.sqltoy.model.Page sqlPage = new org.sagacity.sqltoy.model.Page(page.getSize(), page.getPage());
        Boolean exclusion = query.getExclusion();
        org.sagacity.sqltoy.model.Page<InventoryItemReportDto> findPage;
        if (exclusion != null && exclusion) {
            findPage = lazyDao.findPageBySql(sqlPage, "inventoryItemVoucherList", map, InventoryItemReportDto.class);
        } else {
            findPage = lazyDao.findPageBySql(sqlPage, "inventoryItemReportList", map, InventoryItemReportDto.class);
        }
        return new PageResults<>(findPage.getRows(), page, findPage.getRecordCount());
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
            dto.setOperationType(Objects.requireNonNull(tuple.get(qInventoryItem.operationType.as("operationType"))).name());
            dto.setQuantity(tuple.get(qInventoryItem.quantity.sum().as("quantity")));
            dto.setSubtotal(tuple.get(qInventoryItem.subtotal.sum().as("subtotal")));
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
            inventoryItem.setInventoryDate(new Date());
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
        BooleanExpression expression = qInventoryItem.inventoryDate.in(
                JPAExpressions.select(qInventoryItem.inventoryDate.max().as("inventoryDate")).from(qInventoryItem)
                        .leftJoin(qProduct).on(qInventoryItem.productId.eq(qProduct.id))
                        .leftJoin(qProductCategory).on(qProduct.productCategoryId.eq(qProductCategory.id))
                        .leftJoin(qWarehouse).on(qInventoryItem.warehouseId.eq(qWarehouse.id))
                        .where(qInventoryItem.operationType.notIn(OperationType.期初余额, OperationType.期初库存))
                        .where(qProduct.id.isNotNull(), qWarehouse.id.isNotNull())
                        .orderBy(qInventoryItem.inventoryDate.asc())
                        .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId)
        ).and(qInventoryItem.operationType.notIn(OperationType.期初余额, OperationType.期初库存));
        JPAQuery<Tuple> timerJpaQuery = getBalanceJpaQuery(query, expression);
        JPAQuery<Tuple> tupleJPAQuery = getBalanceJpaQuery(query, expression);
        // 获取日期范围内数据
        BooleanBuilder timerBuilder = new BooleanBuilder();
        Date start = query.getStart();
        Date end = query.getEnd();
        if (start != null) {
            timerBuilder.and(qInventoryItem.inventoryDate.loe(addTimeOfFinalMoment(start)));
            timerBuilder.and(qInventoryItem.inventoryDate.goe(start));
        }
        timerJpaQuery.where(timerBuilder)
                .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId);
        List<Tuple> fetch = timerJpaQuery.fetch();
        // 获取日期开始时间前数据
        List<Tuple> beforeFetch = new ArrayList<>();
        if (start != null && end != null) {
            BooleanBuilder whereBuilder = new BooleanBuilder();
            whereBuilder.and(qInventoryItem.inventoryDate.loe(start));
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
            dto.setInventoryDate(tuple.get(qInventoryItem.inventoryDate.as("inventoryDate")));
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
                        qInventoryItem.inventoryDate.as("inventoryDate"),
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
                .orderBy(qInventoryItem.inventoryDate.asc());
    }

    public List<InventoryItemReportDto> summaryInitial(Query query) {
        Map<String, Object> map = query.toMap();
        Date start = query.getStart();
        map.put("initDate", Objects.requireNonNullElseGet(start, Date::new));
        return lazyDao.findBySql("inventoryItemSummaryInitList", map, InventoryItemReportDto.class);
    }

    public Object reportSummary(Query query) {
        Map<String, Object> map = query.toMap();
        Date end = query.getEnd();
        map.put("initDate", Objects.requireNonNullElseGet(end, Date::new));
        List<InventoryItemReportDto> reportSummary = lazyDao.findBySql("inventoryItemReportSummary", map, InventoryItemReportDto.class);
        return reportSummary.isEmpty() ? null : reportSummary.get(0);
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

        private Long merchantId;

        private Long accountBookId;

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qInventoryItem.merchantId.eq(merchantId));
                this.merchantId = merchantId;
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qInventoryItem.accountBookId.eq(accountBookId));
                this.accountBookId = accountBookId;
            }
        }

        public BooleanBuilder builders() {
            if (start != null && end != null && (isReport == null || !isReport)) {
                builder.and(qInventoryItem.inventoryDate.loe(addTimeOfFinalMoment(end)));
                builder.and(qInventoryItem.inventoryDate.goe(start));
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

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            if (merchantId != null) {
                map.put("merchantId", merchantId);
            }
            if (accountBookId != null) {
                map.put("accountBookId", accountBookId);
            }
            if (start != null && end != null) {
                map.put("startDate", start);
                map.put("endDate", addTimeOfFinalMoment(end));
            }
            if (StrUtil.isNotBlank(filter) && StrUtil.isNotBlank(filter.trim())) {
                map.put("filter", filter);
            }
            if (StrUtil.isNotBlank(summaryFilter) && StrUtil.isNotBlank(summaryFilter.trim())) {
                map.put("summaryFilter", summaryFilter);
            }
            if (warehouseId != null) {
                map.put("warehouseId", warehouseId);
            }
            if (StrUtil.isNotBlank(warehouseIds)) {
                map.put("warehouseIds", warehouseIds);
            }
            if (supplierId != null) {
                map.put("supplierId", supplierId);
            }
            if (StrUtil.isNotBlank(supplierIds)) {
                map.put("supplierIds", supplierIds);
            }
            if (StrUtil.isNotBlank(customerIds)) {
                map.put("customerIds", customerIds);
            }
            if (StrUtil.isNotBlank(productCategoryIds)) {
                map.put("productCategoryIds", productCategoryIds);
            }
            if (productId != null) {
                map.put("productId", productId);
            }
            if (StrUtil.isNotBlank(productIds)) {
                map.put("productIds", productIds);
            }
            if (operationType != null) {
                map.put("operationType", operationType.name());
            }
            if (StrUtil.isNotBlank(operationTypes)) {
                map.put("operationTypes", operationTypes);
            }
            return map;
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
