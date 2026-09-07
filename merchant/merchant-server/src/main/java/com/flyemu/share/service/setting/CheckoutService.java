package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.QOrderPayment;
import com.flyemu.share.entity.fund.QOrderReceipt;
import com.flyemu.share.entity.inventory.*;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.OperationType;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.CheckoutRepository;
import com.flyemu.share.repository.setting.MonthlyCloseLogRepository;
import com.flyemu.share.repository.setting.MonthlyInventorySummaryRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CheckoutService extends BaseService {

    private final static QCheckout qCheckout = QCheckout.checkout;
    private final static QAdmin qAdmin = QAdmin.admin;
    private final static QAccountBook qAccountBook = QAccountBook.accountBook;
    private final static QInventory qInventory = QInventory.inventory;
    private final static QInventoryItem qInventoryItem = QInventoryItem.inventoryItem;
    private final static QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;
    private final static QInventoryTransfer qInventoryTransfer = QInventoryTransfer.inventoryTransfer;
    private final static QStockTake qStockTake = QStockTake.stockTake;
    private final static QOrderReceipt qOrderReceipt = QOrderReceipt.orderReceipt;
    private final static QOrderPayment qOrderPayment = QOrderPayment.orderPayment;
    private final static QProduct qProduct = QProduct.product;
    private final static QWarehouse qWarehouse = QWarehouse.warehouse;
    private final static QUnit qUnit = QUnit.unit;

    private final CheckoutRepository checkoutRepository;
    private final MonthlyCloseLogRepository monthlyCloseLogRepository;
    private final MonthlyInventorySummaryRepository monthlyInventorySummaryRepository;

    /**
     * 分页查询
     */
    public PageResults<Dict> query(Page page, Query query) {
        PagedList<Tuple> fetchPage = bqf.selectFrom(qCheckout)
                .select(qAdmin.name, qCheckout.checkDate, qCheckout.createdAt).where(query.builder)
                .leftJoin(qAdmin).on(qCheckout.checkId.eq(qAdmin.id))
                .orderBy(qCheckout.id.desc())
                .fetchPage(page.getOffset(), page.getOffsetEnd());
        ArrayList<Dict> dicts = fetchPage.stream().collect(ArrayList::new, (list, tuple) -> {
            Dict dict = new Dict().set("checkName", tuple.get(qAdmin.name))
                    .set("checkDate", tuple.get(qCheckout.checkDate))
                    .set("createDate", tuple.get(qCheckout.createdAt));
            list.add(dict);
        }, List::addAll);
        return new PageResults<>(dicts, page, fetchPage.getTotalSize());
    }

    /**
     * 获取账套的启用日期和上次结账日期
     */
    private Tuple fetchAccountBookDates(Long merchantId, Long accountBookId) {
        return bqf.select(qAccountBook.startDate, qAccountBook.checkoutDate)
                .from(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId)
                        .and(qAccountBook.id.eq(accountBookId)))
                .fetchOne();
    }

    /**
     * 共享校验：结账日期参数校验（preCheck 和 save 共用）
     *
     * @return errors 列表，为空表示校验通过
     */
    private List<String> validateCheckoutParams(Long merchantId, Long accountBookId, LocalDate checkDate) {
        List<String> errors = new ArrayList<>();

        Tuple accountBookTuple = fetchAccountBookDates(merchantId, accountBookId);
        if (accountBookTuple == null) {
            errors.add("账套不存在");
            return errors;
        }

        LocalDate startDate = accountBookTuple.get(qAccountBook.startDate);
        LocalDate currentCheckoutDate = accountBookTuple.get(qAccountBook.checkoutDate);

        // 基础日期校验
        if (startDate != null && checkDate.isBefore(startDate)) {
            errors.add("结账日期不能小于系统启用日期：" + startDate);
        }
        if (checkDate.isAfter(LocalDate.now())) {
            errors.add("结账日期不能晚于当前日期");
        }

        // 启用日期变更校验：已有结账记录但启用日期已更新到更晚月份，需先反结账
        if (currentCheckoutDate != null && startDate != null) {
            YearMonth lastClosedMonth = YearMonth.from(currentCheckoutDate);
            YearMonth startMonth = YearMonth.from(startDate);
            if (lastClosedMonth.isBefore(startMonth)) {
                errors.add("启用日期已变更为 " + startMonth.getYear() + "年" + startMonth.getMonthValue()
                        + "月，请先反结账清除旧的结账记录后再重新结账");
                return errors; // 后续校验无意义，直接返回
            }
        }

        // 连续结账校验：不允许跳月
        if (currentCheckoutDate != null) {
            if (!checkDate.isAfter(currentCheckoutDate)) {
                errors.add("结账日期必须大于上次结账日期：" + currentCheckoutDate);
            }
            YearMonth lastClosed = YearMonth.from(currentCheckoutDate);
            YearMonth targetMonth = YearMonth.from(checkDate);
            YearMonth expectedMonth = lastClosed.plusMonths(1);
            if (targetMonth.isBefore(expectedMonth)) {
                errors.add("结账日期不能早于或等于上次结账日期所在月份：" + lastClosed.getYear() + "年" + lastClosed.getMonthValue() + "月");
            } else if (targetMonth.isAfter(expectedMonth)) {
                errors.add("请先完成 " + expectedMonth.getYear() + "年" + expectedMonth.getMonthValue() + "月 的结账，不允许跳月结账");
            }
        } else {
            // 从未结过账，必须从启用日期所在月份开始结账
            if (startDate != null) {
                YearMonth startMonth = YearMonth.from(startDate);
                YearMonth targetMonth = YearMonth.from(checkDate);
                if (!targetMonth.equals(startMonth)) {
                    errors.add("首次结账必须从启用月份开始：" + startMonth.getYear() + "年" + startMonth.getMonthValue() + "月");
                }
            }
        }

        return errors;
    }

    /**
     * 结账前检查 - 返回检查结果
     */
    public Dict preCheck(Long merchantId, Long accountBookId, LocalDate checkDate) {
        Dict result = new Dict();
        List<String> warnings = new ArrayList<>();

        // 共享校验
        List<String> errors = validateCheckoutParams(merchantId, accountBookId, checkDate);

        // 日期校验未通过，提前返回，不做业务检查
        if (!errors.isEmpty()) {
            result.set("success", false);
            result.set("errors", errors);
            result.set("warnings", warnings);
            return result;
        }

        // 计算结账期间范围（月初到月末）
        YearMonth yearMonth = YearMonth.from(checkDate);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = checkDate;

        // 【检查项1】未审核单据检查
        checkUnapprovedDocuments(merchantId, accountBookId, periodStart, periodEnd, errors);

        // 【检查项2】负库存检查
        checkNegativeInventory(merchantId, accountBookId, errors);

        // 【检查项3】负单价/异常成本检查
        checkNegativeCost(merchantId, accountBookId, errors);

        // 【检查项4】零成本出库检查
        checkZeroCostOutbound(merchantId, accountBookId, periodStart, periodEnd, warnings);

        // 【检查项5】未记账资金单据检查
        checkUnapprovedFundDocuments(merchantId, accountBookId, periodStart, periodEnd, errors);

        // 【检查项6】盘点差异未处理检查
        checkStockTakeDifference(merchantId, accountBookId, periodStart, periodEnd, warnings);

        result.set("success", errors.isEmpty());
        result.set("errors", errors);
        result.set("warnings", warnings);
        result.set("checkDate", checkDate);
        result.set("periodStart", periodStart);
        result.set("periodEnd", periodEnd);

        return result;
    }

    /**
     * 【检查项1】未审核单据检查
     */
    private void checkUnapprovedDocuments(Long merchantId, Long accountBookId,
                                           LocalDate periodStart, LocalDate periodEnd,
                                           List<String> errors) {
        // 检查采购入库单
        long purchaseCount = bqf.selectFrom(qPurchaseInbound)
                .where(qPurchaseInbound.merchantId.eq(merchantId)
                        .and(qPurchaseInbound.accountBookId.eq(accountBookId))
                        .and(qPurchaseInbound.inboundDate.goe(periodStart))
                        .and(qPurchaseInbound.inboundDate.loe(periodEnd))
                        .and(qPurchaseInbound.orderStatus.eq(OrderStatus.已保存)))
                .fetchCount();
        if (purchaseCount > 0) {
            errors.add("存在 " + purchaseCount + " 条未审核的采购入库单");
        }

        // 检查销售出库单
        long salesCount = bqf.selectFrom(qSalesOutbound)
                .where(qSalesOutbound.merchantId.eq(merchantId)
                        .and(qSalesOutbound.accountBookId.eq(accountBookId))
                        .and(qSalesOutbound.outboundDate.goe(periodStart))
                        .and(qSalesOutbound.outboundDate.loe(periodEnd))
                        .and(qSalesOutbound.orderStatus.eq(OrderStatus.已保存)))
                .fetchCount();
        if (salesCount > 0) {
            errors.add("存在 " + salesCount + " 条未审核的销售出库单");
        }

        // 检查调拨单 - 使用 transferDate (java.util.Date)
        java.sql.Date periodStartSql = java.sql.Date.valueOf(periodStart);
        java.sql.Date periodEndSql = java.sql.Date.valueOf(periodEnd);

        long transferCount = bqf.selectFrom(qInventoryTransfer)
                .where(qInventoryTransfer.merchantId.eq(merchantId)
                        .and(qInventoryTransfer.accountBookId.eq(accountBookId))
                        .and(qInventoryTransfer.transferDate.goe(periodStartSql))
                        .and(qInventoryTransfer.transferDate.loe(periodEndSql))
                        .and(qInventoryTransfer.orderStatus.eq(OrderStatus.已保存)))
                .fetchCount();
        if (transferCount > 0) {
            errors.add("存在 " + transferCount + " 条未审核的调拨单");
        }

        // 检查盘点单 - 使用 checkDate (java.util.Date)
        long stockTakeCount = bqf.selectFrom(qStockTake)
                .where(qStockTake.merchantId.eq(merchantId)
                        .and(qStockTake.accountBookId.eq(accountBookId))
                        .and(qStockTake.checkDate.goe(periodStartSql))
                        .and(qStockTake.checkDate.loe(periodEndSql))
                        .and(qStockTake.orderStatus.eq(OrderStatus.已保存)))
                .fetchCount();
        if (stockTakeCount > 0) {
            errors.add("存在 " + stockTakeCount + " 条未审核的盘点单");
        }
    }

    /**
     * 【检查项2】负库存检查（批量查询商品名，避免 N+1）
     * <p>
     * 注意：结账恒拦负库存，不随账套参数「可用库存允许为负」放开——
     * 该开关只放行日常单据（出库/货位调拨等）过程中的临时负库存，期末必须清负才能保证账务干净。
     */
    private void checkNegativeInventory(Long merchantId, Long accountBookId, List<String> errors) {
        List<Tuple> negativeList = bqf.select(qInventory.productId, qInventory.currentQuantity)
                .from(qInventory)
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qInventory.currentQuantity.lt(0)))
                .fetch();
        if (CollUtil.isNotEmpty(negativeList)) {
            java.util.Set<Long> productIds = negativeList.stream()
                    .map(t -> t.get(qInventory.productId))
                    .collect(java.util.stream.Collectors.toSet());
            java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
            if (!productIds.isEmpty()) {
                List<Tuple> productTuples = bqf.select(qProduct.id, qProduct.name).from(qProduct)
                        .where(qProduct.id.in(productIds)).fetch();
                for (Tuple pt : productTuples) {
                    nameMap.put(pt.get(qProduct.id), pt.get(qProduct.name));
                }
            }
            StringBuilder sb = new StringBuilder("存在负库存商品：");
            for (Tuple tuple : negativeList) {
                Long productId = tuple.get(qInventory.productId);
                Integer qty = tuple.get(qInventory.currentQuantity);
                sb.append(nameMap.getOrDefault(productId, "未知")).append("(").append(qty).append(") ");
            }
            errors.add(sb.toString());
        }
    }

    /**
     * 【检查项3】负单价/异常成本检查
     */
    private void checkNegativeCost(Long merchantId, Long accountBookId, List<String> errors) {
        long count = bqf.selectFrom(qInventory)
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qInventory.averageCost.lt(BigDecimal.ZERO)))
                .fetchCount();
        if (count > 0) {
            errors.add("存在 " + count + " 个商品成本价为负数，请先处理");
        }
    }

    /**
     * 【检查项4】零成本出库检查
     */
    private void checkZeroCostOutbound(Long merchantId, Long accountBookId,
                                         LocalDate periodStart, LocalDate periodEnd,
                                         List<String> warnings) {
        long count = bqf.selectFrom(qInventoryItem)
                .where(qInventoryItem.merchantId.eq(merchantId)
                        .and(qInventoryItem.accountBookId.eq(accountBookId))
                        .and(qInventoryItem.inventoryDate.goe(java.sql.Date.valueOf(periodStart)))
                        .and(qInventoryItem.inventoryDate.loe(java.sql.Date.valueOf(periodEnd)))
                        .and(qInventoryItem.operationType.in(
                                OperationType.销售出库, OperationType.采购退货,
                                OperationType.调拨出库, OperationType.其他出库, OperationType.盘亏出库))
                        .and(qInventoryItem.subtotal.eq(BigDecimal.ZERO)))
                .fetchCount();
        if (count > 0) {
            warnings.add("存在 " + count + " 条成本为0的出库单据，建议先进行成本核算");
        }
    }

    /**
     * 【检查项5】未记账资金单据检查
     */
    private void checkUnapprovedFundDocuments(Long merchantId, Long accountBookId,
                                               LocalDate periodStart, LocalDate periodEnd,
                                               List<String> errors) {
        // 检查收款单
        long receiptCount = bqf.selectFrom(qOrderReceipt)
                .where(qOrderReceipt.merchantId.eq(merchantId)
                        .and(qOrderReceipt.accountBookId.eq(accountBookId))
                        .and(qOrderReceipt.orderDate.goe(periodStart))
                        .and(qOrderReceipt.orderDate.loe(periodEnd))
                        .and(qOrderReceipt.orderStatus.eq(OrderStatus.已保存)))
                .fetchCount();
        if (receiptCount > 0) {
            errors.add("存在 " + receiptCount + " 条未审核的收款单");
        }

        // 检查付款单
        long paymentCount = bqf.selectFrom(qOrderPayment)
                .where(qOrderPayment.merchantId.eq(merchantId)
                        .and(qOrderPayment.accountBookId.eq(accountBookId))
                        .and(qOrderPayment.orderDate.goe(periodStart))
                        .and(qOrderPayment.orderDate.loe(periodEnd))
                        .and(qOrderPayment.orderStatus.eq(OrderStatus.已保存)))
                .fetchCount();
        if (paymentCount > 0) {
            errors.add("存在 " + paymentCount + " 条未审核的付款单");
        }
    }

    /**
     * 【检查项6】盘点差异未处理检查
     */
    private void checkStockTakeDifference(Long merchantId, Long accountBookId,
                                            LocalDate periodStart, LocalDate periodEnd,
                                            List<String> warnings) {
        // 检查已审核但未生成报损/报溢单的盘点单
        java.sql.Date periodStartSql = java.sql.Date.valueOf(periodStart);
        java.sql.Date periodEndSql = java.sql.Date.valueOf(periodEnd);

        long count = bqf.selectFrom(qStockTake)
                .where(qStockTake.merchantId.eq(merchantId)
                        .and(qStockTake.accountBookId.eq(accountBookId))
                        .and(qStockTake.checkDate.goe(periodStartSql))
                        .and(qStockTake.checkDate.loe(periodEndSql))
                        .and(qStockTake.orderStatus.eq(OrderStatus.已审核)))
                .fetchCount();
        if (count > 0) {
            warnings.add("存在 " + count + " 条已审核的盘点单，请确认差异已处理");
        }
    }

    /**
     * 保存/更新 - 结账执行
     */
    @Transactional
    public Checkout save(Checkout checkout) {
        // 复用共享校验
        List<String> errors = validateCheckoutParams(checkout.getMerchantId(), checkout.getAccountBookId(), checkout.getCheckDate());
        if (CollUtil.isNotEmpty(errors)) {
            throw new ServiceException(String.join("；", errors));
        }

        // 补充校验：商品为负库存时，不允许结账（硬约束，不随「可用库存允许为负」开关放开，与 preCheck 检查项2 同口径）
        long negativeCount = bqf.selectFrom(qInventory)
                .where(qInventory.merchantId.eq(checkout.getMerchantId())
                        .and(qInventory.accountBookId.eq(checkout.getAccountBookId()))
                        .and(qInventory.currentQuantity.lt(0)))
                .fetchCount();
        if (negativeCount > 0) {
            throw new ServiceException("存在 " + negativeCount + " 个负库存商品，不允许结账，请先调整库存数量");
        }

        // 执行结账操作
        Long merchantId = checkout.getMerchantId();
        Long accountBookId = checkout.getAccountBookId();
        LocalDate checkDate = checkout.getCheckDate();

        // 【操作1】生成月结库存表
        generateMonthlyInventorySummary(merchantId, accountBookId, checkDate);

        // 【操作2】更新账套结账日期
        jqf.update(qAccountBook)
                .set(qAccountBook.checkoutDate, checkDate)
                .where(qAccountBook.merchantId.eq(merchantId)
                        .and(qAccountBook.id.eq(accountBookId)))
                .execute();

        // 【操作3】保存结账记录
        if (checkout.getId() != null) {
            Checkout original = checkoutRepository.findById(checkout.getId())
                    .orElseThrow(() -> new ServiceException("结账记录不存在"));
            BeanUtil.copyProperties(checkout, original, CopyOptions.create().ignoreNullValue());
            checkout = checkoutRepository.save(original);
        } else {
            checkout = checkoutRepository.save(checkout);
        }

        // 【操作4】记录结账日志
        MonthlyCloseLog closeLog = new MonthlyCloseLog();
        closeLog.setClosePeriod(checkDate);
        closeLog.setOperatorId(checkout.getCheckId());
        closeLog.setOperatorName(bqf.select(qAdmin.name).from(qAdmin)
                .where(qAdmin.id.eq(checkout.getCheckId())).fetchFirst());
        closeLog.setStatus(1);
        closeLog.setRemark("正常结账");
        closeLog.setMerchantId(merchantId);
        closeLog.setAccountBookId(accountBookId);
        monthlyCloseLogRepository.save(closeLog);

        log.info("结账完成：商户ID={}, 账套ID={}, 结账日期={}", merchantId, accountBookId, checkDate);

        return checkout;
    }

    /**
     * 生成月结库存表
     */
    /**
     * 入库类型操作（public 供成本法切换等复用判定「期间内库存流水」）
     */
    public static final java.util.Set<OperationType> INBOUND_TYPES = java.util.Set.of(
            OperationType.期初余额, OperationType.采购入库, OperationType.销售退货,
            OperationType.其他入库, OperationType.盘盈入库, OperationType.调拨入库,
            OperationType.期初库存, OperationType.组装入库, OperationType.拆卸入库
    );

    /**
     * 出库类型操作（public 供成本法切换等复用判定「期间内库存流水」）
     */
    public static final java.util.Set<OperationType> OUTBOUND_TYPES = java.util.Set.of(
            OperationType.采购退货, OperationType.销售出库, OperationType.其他出库,
            OperationType.调拨出库, OperationType.盘亏出库,
            OperationType.组装出库, OperationType.拆卸出库
    );

    private void generateMonthlyInventorySummary(Long merchantId, Long accountBookId, LocalDate checkDate) {
        YearMonth yearMonth = YearMonth.from(checkDate);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = checkDate;

        // 【防重复】先删除本期已有的月结数据
        monthlyInventorySummaryRepository.deleteByMerchantIdAndAccountBookIdAndPeriod(
                merchantId, accountBookId, checkDate);

        // 上期期末作为本期期初，用 Map 关联（key = productId * 10000 + warehouseId）
        LocalDate lastPeriod = periodStart.minusDays(1);
        List<MonthlyInventorySummary> lastPeriodData = monthlyInventorySummaryRepository
                .findByMerchantIdAndAccountBookIdAndPeriodOrderByProductIdAscWarehouseIdAsc(merchantId, accountBookId, lastPeriod);
        java.util.Map<Long, MonthlyInventorySummary> lastPeriodMap = new java.util.HashMap<>();
        for (MonthlyInventorySummary s : lastPeriodData) {
            lastPeriodMap.put(s.getProductId() * 10000L + s.getWarehouseId(), s);
        }

        // 本期库存明细：按商品+仓库+操作类型分组，分别累计数量和金额
        List<Tuple> inventoryItems = bqf.select(
                        qInventoryItem.productId,
                        qInventoryItem.warehouseId,
                        qInventoryItem.operationType,
                        qInventoryItem.quantity.sum().as("totalQty"),
                        qInventoryItem.subtotal.sum().as("totalAmount"))
                .from(qInventoryItem)
                .where(qInventoryItem.merchantId.eq(merchantId)
                        .and(qInventoryItem.accountBookId.eq(accountBookId))
                        .and(qInventoryItem.inventoryDate.goe(java.sql.Date.valueOf(periodStart)))
                        .and(qInventoryItem.inventoryDate.loe(java.sql.Date.valueOf(periodEnd))))
                .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId, qInventoryItem.operationType)
                .fetch();

        // 按商品+仓库聚合入库/出库
        java.util.Map<Long, BigDecimal[]> flowMap = new java.util.HashMap<>(); // [inQty, inAmt, outQty, outAmt]
        java.util.Set<Long> allProductIds = new java.util.HashSet<>();
        java.util.Set<Long> allWarehouseIds = new java.util.HashSet<>();

        for (Tuple tuple : inventoryItems) {
            Long productId = tuple.get(qInventoryItem.productId);
            Long warehouseId = tuple.get(qInventoryItem.warehouseId);
            OperationType opType = tuple.get(qInventoryItem.operationType);
            Integer qty = tuple.get(qInventoryItem.quantity.sum().as("totalQty"));
            BigDecimal amount = tuple.get(qInventoryItem.subtotal.sum().as("totalAmount"));

            if (qty == null) qty = 0;
            if (amount == null) amount = BigDecimal.ZERO;

            allProductIds.add(productId);
            allWarehouseIds.add(warehouseId);

            long key = productId * 10000L + warehouseId;
            BigDecimal[] flows = flowMap.computeIfAbsent(key, k -> new BigDecimal[]{
                    BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});

            if (INBOUND_TYPES.contains(opType)) {
                flows[0] = flows[0].add(new BigDecimal(qty));   // 入库数量
                flows[1] = flows[1].add(amount);                // 入库金额
            } else if (OUTBOUND_TYPES.contains(opType)) {
                flows[2] = flows[2].add(new BigDecimal(Math.abs(qty))); // 出库数量
                flows[3] = flows[3].add(amount.abs());                   // 出库金额
            }
        }

        // 批量查询商品、仓库、单位信息（避免 N+1）
        java.util.Map<Long, Product> productMap = new java.util.HashMap<>();
        if (!allProductIds.isEmpty()) {
            List<Product> products = bqf.selectFrom(qProduct)
                    .where(qProduct.id.in(allProductIds)).fetch();
            products.forEach(p -> productMap.put(p.getId(), p));
        }
        java.util.Map<Long, Warehouse> warehouseMap = new java.util.HashMap<>();
        if (!allWarehouseIds.isEmpty()) {
            List<Warehouse> warehouses = bqf.selectFrom(qWarehouse)
                    .where(qWarehouse.id.in(allWarehouseIds)).fetch();
            warehouses.forEach(w -> warehouseMap.put(w.getId(), w));
        }
        java.util.Map<Long, String> unitNameMap = new java.util.HashMap<>();
        java.util.Set<Long> unitIds = productMap.values().stream()
                .filter(p -> p.getUnitId() != null)
                .map(Product::getUnitId)
                .collect(java.util.stream.Collectors.toSet());
        if (!unitIds.isEmpty()) {
            List<Unit> units = bqf.selectFrom(qUnit).where(qUnit.id.in(unitIds)).fetch();
            units.forEach(u -> unitNameMap.put(u.getId(), u.getName()));
        }

        // 生成月结数据
        List<MonthlyInventorySummary> summaryList = new ArrayList<>();
        for (java.util.Map.Entry<Long, BigDecimal[]> entry : flowMap.entrySet()) {
            long key = entry.getKey();
            BigDecimal[] flows = entry.getValue();
            long productId = key / 10000L;
            long warehouseId = key % 10000L;

            BigDecimal inQty = flows[0];
            BigDecimal inAmount = flows[1];
            BigDecimal outQty = flows[2];
            BigDecimal outAmount = flows[3];

            // 期初：从上期期末获取
            BigDecimal beginQty = BigDecimal.ZERO;
            BigDecimal beginAmount = BigDecimal.ZERO;
            MonthlyInventorySummary lastSummary = lastPeriodMap.get(key);
            if (lastSummary != null) {
                beginQty = lastSummary.getEndQty();
                beginAmount = lastSummary.getEndAmount();
            }

            // 期末 = 期初 + 入库 - 出库
            BigDecimal endQty = beginQty.add(inQty).subtract(outQty);
            BigDecimal endAmount = beginAmount.add(inAmount).subtract(outAmount);

            Product product = productMap.get(productId);
            Warehouse warehouse = warehouseMap.get(warehouseId);
            String unitName = (product != null && product.getUnitId() != null)
                    ? unitNameMap.getOrDefault(product.getUnitId(), "") : "";

            MonthlyInventorySummary summary = new MonthlyInventorySummary();
            summary.setProductId(productId);
            summary.setWarehouseId(warehouseId);
            summary.setPeriod(checkDate);
            summary.setBeginQty(beginQty);
            summary.setBeginAmount(beginAmount);
            summary.setInQty(inQty);
            summary.setInAmount(inAmount);
            summary.setOutQty(outQty);
            summary.setOutAmount(outAmount);
            summary.setEndQty(endQty);
            summary.setEndAmount(endAmount);
            summary.setProductCode(product != null ? product.getCode() : "");
            summary.setProductName(product != null ? product.getName() : "");
            summary.setWarehouseCode(warehouse != null ? warehouse.getCode() : "");
            summary.setWarehouseName(warehouse != null ? warehouse.getName() : "");
            summary.setUnitName(unitName);
            summary.setMerchantId(merchantId);
            summary.setAccountBookId(accountBookId);
            summaryList.add(summary);
        }

        if (CollUtil.isNotEmpty(summaryList)) {
            monthlyInventorySummaryRepository.saveAll(summaryList);
            log.info("生成月结库存表：{} 条记录，期间={}~{}", summaryList.size(), periodStart, periodEnd);
        }
    }

    /**
     * 反结账
     *
     * @param accountBookId 账套ID
     * @param merchantId    商户ID
     * @param operatorId    操作人ID
     * @return 反结账后的账套结账日期
     */
    @Transactional
    public LocalDate cancelCheckout(Long accountBookId, Long merchantId, Long operatorId) {
        // 查询所有结账记录，按日期倒序
        List<Checkout> checkouts = bqf.selectFrom(qCheckout)
                .where(qCheckout.accountBookId.eq(accountBookId).and(qCheckout.merchantId.eq(merchantId)))
                .orderBy(qCheckout.checkDate.desc()).fetch();

        if (CollUtil.isEmpty(checkouts)) {
            throw new ServiceException("没有可反结账的记录");
        }

        Checkout latestCheckout = checkouts.get(0);
        LocalDate cancelPeriod = latestCheckout.getCheckDate();

        // 获取操作人姓名
        String operatorName = bqf.select(qAdmin.name).from(qAdmin)
                .where(qAdmin.id.eq(operatorId)).fetchFirst();

        if (checkouts.size() > 1) {
            // 有上一期：反结账后恢复到上一期的结账日期
            Checkout previousCheckout = checkouts.get(1);
            LocalDate restoreDate = previousCheckout.getCheckDate();

            // 删除最新一期的月结数据
            monthlyInventorySummaryRepository.deleteByMerchantIdAndAccountBookIdAndPeriod(
                    merchantId, accountBookId, cancelPeriod);

            // 删除结账记录
            checkoutRepository.deleteById(latestCheckout.getId());

            // 更新账套结账日期为上一期
            jqf.update(qAccountBook)
                    .set(qAccountBook.checkoutDate, restoreDate)
                    .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                    .execute();

            // 删除被撤销期间的正向结账日志，记录反结账日志
            monthlyCloseLogRepository.deleteByMerchantIdAndAccountBookIdAndClosePeriod(
                    merchantId, accountBookId, cancelPeriod);
            saveAntiCheckoutLog(merchantId, accountBookId, cancelPeriod, operatorId, operatorName);

            log.info("反结账完成：商户ID={}, 账套ID={}, 撤销期间={}, 恢复到={}", merchantId, accountBookId, cancelPeriod, restoreDate);
            return restoreDate;
        } else {
            // 只有一条记录：反结账后恢复到启用日期
            LocalDate startDate = jqf.select(qAccountBook.startDate).from(qAccountBook)
                    .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                    .fetchFirst();

            // 删除月结数据和结账记录
            monthlyInventorySummaryRepository.deleteByMerchantIdAndAccountBookIdAndPeriod(
                    merchantId, accountBookId, cancelPeriod);
            checkoutRepository.deleteById(latestCheckout.getId());

            // 清空账套结账日期：没有任何生效结账记录，必须置空。
            // 之前误写为 startDate（非空锚点），会导致各处“checkout_date != null”判断把该账套误判为已结账，
            // 期初余额被禁止修改、重新结账首个启用月也被误判为“上次已结账过”而拒绝。
            jqf.update(qAccountBook)
                    .setNull(qAccountBook.checkoutDate)
                    .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                    .execute();

            // 删除正向结账日志，记录反结账日志
            monthlyCloseLogRepository.deleteByMerchantIdAndAccountBookIdAndClosePeriod(
                    merchantId, accountBookId, cancelPeriod);
            saveAntiCheckoutLog(merchantId, accountBookId, cancelPeriod, operatorId, operatorName);

            log.info("反结账完成（最后一条）：商户ID={}, 账套ID={}, 恢复到启用日期={}", merchantId, accountBookId, startDate);
            return startDate;
        }
    }

    /**
     * 保存反结账日志
     */
    private void saveAntiCheckoutLog(Long merchantId, Long accountBookId, LocalDate closePeriod,
                                     Long operatorId, String operatorName) {
        MonthlyCloseLog closeLog = new MonthlyCloseLog();
        closeLog.setClosePeriod(closePeriod);
        closeLog.setOperatorId(operatorId);
        closeLog.setOperatorName(operatorName != null ? operatorName : "未知");
        closeLog.setStatus(-1);
        closeLog.setRemark("反结账");
        closeLog.setMerchantId(merchantId);
        closeLog.setAccountBookId(accountBookId);
        monthlyCloseLogRepository.save(closeLog);
    }

    /**
     * 账套当前是否处于「已结账」状态（存在生效中的结账记录）。
     * <p>
     * 生效结账以 jxc_checkout 记录为准（结账时新增、反结账时删除），不依赖 account_book.checkout_date 是否为空：
     * 全部反结账后 checkout_date 应已清空为 null，但历史遗留/老代码可能残留 startDate 等非空值，
     * 若只看“checkout_date != null”会把这些残留误判为已结账，导致期初余额被错误禁止修改。
     */
    public boolean isCheckedOut(Long merchantId, Long accountBookId) {
        Long count = bqf.selectFrom(qCheckout)
                .where(qCheckout.merchantId.eq(merchantId)
                        .and(qCheckout.accountBookId.eq(accountBookId)))
                .fetchCount();
        return count != null && count > 0;
    }

    /**
     * 断言单据可编辑
     */
    public void assertEditable(Long merchantId, Long accountBookId, LocalDate orderDate) {
        if (orderDate == null) {
            throw new ServiceException("单据日期不能为空");
        }
        LocalDate checkoutDate = jqf.select(qAccountBook.checkoutDate).from(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .fetchOne();
        if (checkoutDate != null && !orderDate.isAfter(checkoutDate)) {
            throw new ServiceException("单据日期不能早于或等于结账日期：" + checkoutDate + "，无法操作已结账期间的单据");
        }
    }

    /**
     * 查询月结库存表
     */
    public List<MonthlyInventorySummary> queryMonthlySummary(Long merchantId, Long accountBookId, LocalDate period) {
        return monthlyInventorySummaryRepository
                .findByMerchantIdAndAccountBookIdAndPeriodOrderByProductIdAscWarehouseIdAsc(merchantId, accountBookId, period);
    }

    /**
     * 查询结账历史
     */
    public List<MonthlyCloseLog> queryCloseHistory(Long merchantId, Long accountBookId) {
        return monthlyCloseLogRepository
                .findByMerchantIdAndAccountBookIdOrderByClosePeriodDesc(merchantId, accountBookId);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qCheckout.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qCheckout.accountBookId, accountBookId);
        }
    }
}
