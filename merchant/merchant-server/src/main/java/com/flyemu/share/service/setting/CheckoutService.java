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
     * 结账前检查 - 返回检查结果
     */
    public Dict preCheck(Long merchantId, Long accountBookId, LocalDate checkDate) {
        Dict result = new Dict();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // 获取账套信息
        Tuple accountBookTuple = bqf.select(qAccountBook.startDate, qAccountBook.checkoutDate)
                .from(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId)
                        .and(qAccountBook.id.eq(accountBookId)))
                .fetchOne();

        if (accountBookTuple == null) {
            errors.add("账套不存在");
            result.set("success", false);
            result.set("errors", errors);
            return result;
        }

        LocalDate startDate = accountBookTuple.get(qAccountBook.startDate);
        LocalDate currentCheckoutDate = accountBookTuple.get(qAccountBook.checkoutDate);

        // 基础日期校验
        if (startDate != null && checkDate.isBefore(startDate)) {
            errors.add("结账日期不能小于系统启用日期：" + startDate);
        }
        if (currentCheckoutDate != null && !checkDate.isAfter(currentCheckoutDate)) {
            errors.add("结账日期必须大于上次结账日期：" + currentCheckoutDate);
        }
        if (checkDate.isAfter(LocalDate.now())) {
            errors.add("结账日期不能晚于当前日期");
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
     * 【检查项2】负库存检查
     */
    private void checkNegativeInventory(Long merchantId, Long accountBookId, List<String> errors) {
        List<Tuple> negativeList = bqf.select(qInventory.productId, qInventory.warehouseId, qInventory.currentQuantity)
                .from(qInventory)
                .where(qInventory.merchantId.eq(merchantId)
                        .and(qInventory.accountBookId.eq(accountBookId))
                        .and(qInventory.currentQuantity.lt(0)))
                .fetch();
        if (CollUtil.isNotEmpty(negativeList)) {
            StringBuilder sb = new StringBuilder("存在负库存商品：");
            for (Tuple tuple : negativeList) {
                Long productId = tuple.get(qInventory.productId);
                Integer qty = tuple.get(qInventory.currentQuantity);
                String productName = bqf.select(qProduct.name).from(qProduct)
                        .where(qProduct.id.eq(productId)).fetchFirst();
                sb.append(productName).append("(").append(qty).append(") ");
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
        List<String> strings = new ArrayList<>();

        // 获取账套信息进行校验
        Tuple accountBookTuple = bqf.select(qAccountBook.startDate, qAccountBook.checkoutDate)
                .from(qAccountBook)
                .where(qAccountBook.merchantId.eq(checkout.getMerchantId())
                        .and(qAccountBook.id.eq(checkout.getAccountBookId())))
                .fetchOne();

        if (accountBookTuple == null) {
            throw new ServiceException("账套不存在");
        }

        LocalDate startDate = accountBookTuple.get(qAccountBook.startDate);
        LocalDate currentCheckoutDate = accountBookTuple.get(qAccountBook.checkoutDate);

        // 校验1：结账日期不能小于系统启用日期
        if (startDate != null && checkout.getCheckDate().isBefore(startDate)) {
            strings.add("结账日期不能小于系统启用日期：" + startDate);
        }

        // 校验2：结账日期不能小于或等于上次结账日期
        if (currentCheckoutDate != null && !checkout.getCheckDate().isAfter(currentCheckoutDate)) {
            strings.add("结账日期必须大于上次结账日期：" + currentCheckoutDate);
        }

        // 校验3：结账日期不能早于今天（不允许未来日期结账）
        if (checkout.getCheckDate().isAfter(LocalDate.now())) {
            strings.add("结账日期不能晚于当前日期");
        }

        // 校验4：商品为负库存时，不允许结账
        List<Tuple> negativeInventoryProducts = bqf.select(qInventory.productId, qInventory.currentQuantity)
                .from(qInventory)
                .where(qInventory.merchantId.eq(checkout.getMerchantId())
                        .and(qInventory.accountBookId.eq(checkout.getAccountBookId()))
                        .and(qInventory.currentQuantity.lt(0)))
                .fetch();
        if (CollUtil.isNotEmpty(negativeInventoryProducts)) {
            strings.add("存在负库存商品，不允许结账。请先调整库存数量");
        }

        if (CollUtil.isNotEmpty(strings)) {
            throw new ServiceException(String.join("；", strings));
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
    private void generateMonthlyInventorySummary(Long merchantId, Long accountBookId, LocalDate checkDate) {
        // 计算上月月末（用于获取期初数据）
        YearMonth yearMonth = YearMonth.from(checkDate);
        LocalDate periodStart = yearMonth.atDay(1);
        LocalDate periodEnd = checkDate;

        // 获取上期月结数据作为本期期初
        LocalDate lastPeriod = periodStart.minusDays(1);
        List<MonthlyInventorySummary> lastPeriodData = monthlyInventorySummaryRepository
                .findByMerchantIdAndAccountBookIdAndPeriodOrderByProductIdAscWarehouseIdAsc(merchantId, accountBookId, lastPeriod);

        // 获取本期库存明细（按商品+仓库分组）
        List<Tuple> inventoryItems = bqf.select(
                        qInventoryItem.productId,
                        qInventoryItem.warehouseId,
                        qInventoryItem.quantity.sum().as("totalQty"),
                        qInventoryItem.subtotal.sum().as("totalAmount"))
                .from(qInventoryItem)
                .where(qInventoryItem.merchantId.eq(merchantId)
                        .and(qInventoryItem.accountBookId.eq(accountBookId))
                        .and(qInventoryItem.inventoryDate.goe(java.sql.Date.valueOf(periodStart)))
                        .and(qInventoryItem.inventoryDate.loe(java.sql.Date.valueOf(periodEnd))))
                .groupBy(qInventoryItem.productId, qInventoryItem.warehouseId)
                .fetch();

        // 生成月结数据
        List<MonthlyInventorySummary> summaryList = new ArrayList<>();

        for (Tuple tuple : inventoryItems) {
            Long productId = tuple.get(qInventoryItem.productId);
            Long warehouseId = tuple.get(qInventoryItem.warehouseId);
            Integer totalQty = tuple.get(qInventoryItem.quantity.sum().as("totalQty"));
            BigDecimal totalAmount = tuple.get(qInventoryItem.subtotal.sum().as("totalAmount"));

            if (totalQty == null) totalQty = 0;
            if (totalAmount == null) totalAmount = BigDecimal.ZERO;

            // 查找期初数据
            BigDecimal beginQty = BigDecimal.ZERO;
            BigDecimal beginAmount = BigDecimal.ZERO;
            for (MonthlyInventorySummary last : lastPeriodData) {
                if (last.getProductId().equals(productId) && last.getWarehouseId().equals(warehouseId)) {
                    beginQty = last.getEndQty();
                    beginAmount = last.getEndAmount();
                    break;
                }
            }

            // 计算入库/出库
            BigDecimal inQty = BigDecimal.ZERO;
            BigDecimal inAmount = BigDecimal.ZERO;
            BigDecimal outQty = BigDecimal.ZERO;
            BigDecimal outAmount = BigDecimal.ZERO;

            if (totalQty > 0) {
                inQty = new BigDecimal(totalQty);
                inAmount = totalAmount;
            } else {
                outQty = new BigDecimal(Math.abs(totalQty));
                outAmount = totalAmount.abs();
            }

            // 计算期末
            BigDecimal endQty = beginQty.add(inQty).subtract(outQty);
            BigDecimal endAmount = beginAmount.add(inAmount).subtract(outAmount);

            // 获取商品信息
            Product product = bqf.selectFrom(qProduct).where(qProduct.id.eq(productId)).fetchFirst();
            Warehouse warehouse = bqf.selectFrom(qWarehouse).where(qWarehouse.id.eq(warehouseId)).fetchFirst();
            Unit unit = null;
            if (product != null && product.getUnitId() != null) {
                unit = bqf.selectFrom(qUnit).where(qUnit.id.eq(product.getUnitId())).fetchFirst();
            }

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
            summary.setUnitName(unit != null ? unit.getName() : "");
            summary.setMerchantId(merchantId);
            summary.setAccountBookId(accountBookId);

            summaryList.add(summary);
        }

        // 保存月结数据
        if (CollUtil.isNotEmpty(summaryList)) {
            monthlyInventorySummaryRepository.saveAll(summaryList);
            log.info("生成月结库存表：{} 条记录", summaryList.size());
        }
    }

    /**
     * 反结账
     */
    @Transactional
    public LocalDate cancelCheckout(Long accountBookId, Long merchantId) {
        List<Checkout> checkouts = bqf.selectFrom(qCheckout)
                .where(qCheckout.accountBookId.eq(accountBookId).and(qCheckout.merchantId.eq(merchantId)))
                .orderBy(qCheckout.checkDate.desc()).fetch().stream().limit(2).collect(Collectors.toList());

        if (CollUtil.isNotEmpty(checkouts) && checkouts.size() > 1) {
            // 删除最新一期的月结数据
            Checkout latestCheckout = checkouts.get(0);
            monthlyInventorySummaryRepository.deleteByMerchantIdAndAccountBookIdAndPeriod(
                    merchantId, accountBookId, latestCheckout.getCheckDate());

            // 删除结账记录
            checkoutRepository.deleteById(checkouts.get(0).getId());

            // 更新账套结账日期为上一期
            jqf.update(qAccountBook)
                    .set(qAccountBook.checkoutDate, checkouts.get(1).getCheckDate())
                    .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                    .execute();

            // 记录反结账日志
            MonthlyCloseLog closeLog = new MonthlyCloseLog();
            closeLog.setClosePeriod(latestCheckout.getCheckDate());
            closeLog.setOperatorId(0L); // 系统操作
            closeLog.setOperatorName("系统");
            closeLog.setStatus(-1);
            closeLog.setRemark("反结账");
            closeLog.setMerchantId(merchantId);
            closeLog.setAccountBookId(accountBookId);
            monthlyCloseLogRepository.save(closeLog);

            return checkouts.get(1).getCheckDate();
        } else {
            if (CollUtil.isNotEmpty(checkouts)) {
                checkoutRepository.deleteById(checkouts.get(0).getId());
            }
            LocalDate checkDate = bqf.selectFrom(qAccountBook)
                    .select(qAccountBook.startDate)
                    .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                    .fetchFirst();
            jqf.update(qAccountBook)
                    .set(qAccountBook.checkoutDate, checkDate)
                    .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                    .execute();
            return checkDate;
        }
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
