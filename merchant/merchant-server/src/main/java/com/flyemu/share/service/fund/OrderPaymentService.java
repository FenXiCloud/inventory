package com.flyemu.share.service.fund;

import com.flyemu.share.repository.fund.OrderPaymentCollectionRepository;
import com.flyemu.share.repository.fund.OrderPaymentItemRepository;
import com.flyemu.share.repository.fund.OrderPaymentRepository;
import com.flyemu.share.repository.purchase.PurchaseInboundRepository;
import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.entity.purchase.PurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseInbound;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.basic.SupplierService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.form.OrderPaymentForm;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.vo.OrderPaymentDetails;
import com.flyemu.share.service.fund.vo.OrderPaymentDetailsVO;
import com.flyemu.share.service.fund.vo.OrderPaymentQueryVO;
import com.flyemu.share.service.fund.vo.report.PayableDetailReportVO;
import com.flyemu.share.service.fund.vo.report.SummaryPayableDetailsPageVO;
import com.flyemu.share.service.fund.vo.report.SummaryPayableDetailsVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QOrderPayment qOrderPayment = QOrderPayment.orderPayment;
    private final static QOrderPaymentItem qOrderPaymentItem = QOrderPaymentItem.orderPaymentItem;
    private final static QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
    private final static QVerification qVerification = QVerification.verification;

    private final OrderPaymentRepository orderPaymentRepository;
    private final static QOrderPaymentItem qItem = QOrderPaymentItem.orderPaymentItem;
    private final static QOrderPaymentCollection qCollection = QOrderPaymentCollection.orderPaymentCollection;
    private final static QSupplier qSupplier = QSupplier.supplier;
    private final static QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;
    private final static QOrderStaff qOrderStaff = QOrderStaff.orderStaff;
    private final static QPurchaseInbound qPurchaseOrderInbound = QPurchaseInbound.purchaseInbound;

    private final static QOtherExpense qOtherExpense = QOtherExpense.otherExpense;
    private final CodeRuleService codeRuleService;
    private final AccountService accountService;
    private final SupplierService supplierService;
    private final PurchaseInboundRepository quantityRepository;
    private final OrderPaymentItemRepository orderPaymentItemRepository;
    private final OrderPaymentCollectionRepository orderPaymentCollectionRepository;
    private final SettlementService settlementService;
    private final static QOrderPaymentItem QorderPaymentItem = QOrderPaymentItem.orderPaymentItem;
    private final static QOrderPaymentCollection QorderPaymentCollection = QOrderPaymentCollection.orderPaymentCollection;

    public SummaryPayableDetailsPageVO summaryPayableDetails(Page page, SummaryPayableDetailsQuery query) {
        Integer type = query.getType();
        if (type == null || type < 1 || type > 3) {
            throw new ServiceException("type 参数必须为 1、2 或 3");
        }
        if (query.getStartDate() == null || query.getEndDate() == null) {
            throw new ServiceException("startDate 和 endDate 参数不能为空");
        }
        return switch (type) {
            case 1 -> handleBySupplier(page, query);
            case 2 -> handleBySupplierCategory(page, query);
            case 3 -> handleByStaff(page, query);
            default -> throw new ServiceException("不支持的查询类型");
        };
    }

    private SummaryPayableDetailsPageVO handleBySupplier(Page page, SummaryPayableDetailsQuery query) {
        QSupplier qSupplier = QSupplier.supplier;

        BooleanBuilder supplierCondition = new BooleanBuilder();
        if (query.getMerchantId() != null) {
            supplierCondition.and(qSupplier.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            supplierCondition.and(qSupplier.accountBookId.eq(query.getAccountBookId()));
        }
        if (query.getSupplierId() != null) {
            supplierCondition.and(qSupplier.id.eq(query.getSupplierId()));
        }

        LocalDateTime startDateTime = query.getStartDate().atStartOfDay();

        JPAQuery<Supplier> supplierQuery = jqf.selectFrom(qSupplier).where(supplierCondition);
        long total = supplierQuery.fetchCount();

        List<Supplier> suppliers = supplierQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();

        if (suppliers.isEmpty()) {
            return new SummaryPayableDetailsPageVO();
        }

        List<SummaryPayableDetailsVO> voList = new ArrayList<>();
        SummaryPayableDetailsPageVO totalVO = new SummaryPayableDetailsPageVO();
        totalVO.setTotalOpeningBalance(BigDecimal.ZERO);
        totalVO.setTotalCurrentPayable(BigDecimal.ZERO);
        totalVO.setTotalCurrentPayment(BigDecimal.ZERO);
        totalVO.setTotalClosingBalance(BigDecimal.ZERO);

        for (Supplier supplier : suppliers) {
            SummaryPayableDetailsVO vo = new SummaryPayableDetailsVO();
            SupplierCategory byId = jqf.select(qSupplierCategory).from(qSupplierCategory).where(qSupplierCategory.id.eq(supplier.getSupplierCategoryId())).fetchFirst();
            if (byId != null) {
                vo.setSupplierCategory(byId.getName());
            }
            vo.setSupplierCode(supplier.getCode());
            vo.setSupplierName(supplier.getName());

            BigDecimal openingBalance = getOpeningBalance(supplier.getId(), startDateTime);
            BigDecimal currentPayable = getCurrentPayable(supplier.getId(), query);
            BigDecimal currentPayment = getCurrentPayment(supplier.getId(), query);
            BigDecimal closingBalance = openingBalance.add(currentPayable).subtract(currentPayment);

            vo.setOpeningBalance(openingBalance);
            vo.setCurrentPayable(currentPayable);
            vo.setCurrentPayment(currentPayment);
            vo.setClosingBalance(closingBalance);

            voList.add(vo);

            totalVO.setTotalOpeningBalance(totalVO.getTotalOpeningBalance().add(openingBalance));
            totalVO.setTotalCurrentPayable(totalVO.getTotalCurrentPayable().add(currentPayable));
            totalVO.setTotalCurrentPayment(totalVO.getTotalCurrentPayment().add(currentPayment));
            totalVO.setTotalClosingBalance(totalVO.getTotalClosingBalance().add(closingBalance));
        }

        totalVO.setPayableDetailsList(voList);
        totalVO.setPayableDetailsListTotal((int) total);
        return totalVO;
    }

    private SummaryPayableDetailsPageVO handleBySupplierCategory(Page page, SummaryPayableDetailsQuery query) {
        QSupplierCategory qSupplierCategory = QSupplierCategory.supplierCategory;

        BooleanBuilder condition = new BooleanBuilder();
        if (query.getMerchantId() != null) {
            condition.and(qSupplierCategory.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            condition.and(qSupplierCategory.accountBookId.eq(query.getAccountBookId()));
        }
        if (query.getSupplierTypeId() != null) {
            condition.and(qSupplierCategory.id.eq(query.getSupplierTypeId()));
        }
        JPAQuery<SupplierCategory> categoryQuery = jqf.select(qSupplierCategory).from(qSupplierCategory).where(condition).orderBy(qSupplierCategory.id.asc());

        long total = categoryQuery.fetchCount();

        List<SupplierCategory> categories = categoryQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();

        List<SummaryPayableDetailsVO> voList = new ArrayList<>();

        for (SupplierCategory category : categories) {
            String categoryName = category.getName();

            List<Long> supplierIds = jqf.select(QSupplier.supplier.id).from(QSupplier.supplier).where(QSupplier.supplier.supplierCategoryId.eq(category.getId()).and(QSupplier.supplier.merchantId.eq(query.getMerchantId())).and(QSupplier.supplier.accountBookId.eq(query.getAccountBookId()))).fetch();

            if (supplierIds.isEmpty()) {
                continue;
            }

            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentPayable = BigDecimal.ZERO;
            BigDecimal currentPayment = BigDecimal.ZERO;

            LocalDateTime startDateTime = query.getStartDate().atStartOfDay();

            for (Long supplierId : supplierIds) {
                openingBalance = openingBalance.add(getOpeningBalance(supplierId, startDateTime));
                currentPayable = currentPayable.add(getCurrentPayable(supplierId, query));
                currentPayment = currentPayment.add(getCurrentPayment(supplierId, query));
            }

            BigDecimal closingBalance = openingBalance.add(currentPayable).subtract(currentPayment);

            SummaryPayableDetailsVO vo = new SummaryPayableDetailsVO();
            vo.setSupplierCategory(categoryName);
            vo.setOpeningBalance(openingBalance);
            vo.setCurrentPayable(currentPayable);
            vo.setCurrentPayment(currentPayment);
            vo.setClosingBalance(closingBalance);

            voList.add(vo);
        }

        SummaryPayableDetailsPageVO result = new SummaryPayableDetailsPageVO();
        result.setTotalOpeningBalance(BigDecimal.ZERO);
        result.setTotalCurrentPayable(BigDecimal.ZERO);
        result.setTotalCurrentPayment(BigDecimal.ZERO);
        result.setTotalClosingBalance(BigDecimal.ZERO);

        for (SummaryPayableDetailsVO vo : voList) {
            result.setTotalOpeningBalance(result.getTotalOpeningBalance().add(vo.getOpeningBalance()));
            result.setTotalCurrentPayable(result.getTotalCurrentPayable().add(vo.getCurrentPayable()));
            result.setTotalCurrentPayment(result.getTotalCurrentPayment().add(vo.getCurrentPayment()));
            result.setTotalClosingBalance(result.getTotalClosingBalance().add(vo.getClosingBalance()));
        }
        result.setPayableDetailsList(voList);
        result.setPayableDetailsListTotal(total);
        return result;
    }

    private SummaryPayableDetailsPageVO handleByStaff(Page page, SummaryPayableDetailsQuery query) {
        QOrderStaff qOrderStaff = QOrderStaff.orderStaff;

        BooleanBuilder condition = new BooleanBuilder();
        if (query.getMerchantId() != null) {
            condition.and(qOrderStaff.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            condition.and(qOrderStaff.accountBookId.eq(query.getAccountBookId()));
        }
        if (query.getSalesmanId() != null) {
            condition.and(qOrderStaff.id.eq(query.getSalesmanId()));
        }
        JPAQuery<OrderStaff> staffQuery = jqf.selectFrom(qOrderStaff).where(condition);
        long total = staffQuery.fetchCount();
        List<OrderStaff> staffList = staffQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();

        List<SummaryPayableDetailsVO> voList = new ArrayList<>();

        for (OrderStaff staff : staffList) {
            Long staffId = staff.getId().longValue();
            String staffName = staff.getName();

            List<Long> paymentIds = jqf.select(qOrderPayment.id).from(qOrderPayment).where(qOrderPayment.orderStaffId.eq(staffId).and(qOrderPayment.merchantId.eq(query.getMerchantId())).and(qOrderPayment.accountBookId.eq(query.getAccountBookId()))).fetch();

            if (paymentIds.isEmpty()) {
                SummaryPayableDetailsVO vo = new SummaryPayableDetailsVO();
                vo.setSupplierName(staffName);
                vo.setSupplierCode(String.valueOf(staffId));
                vo.setOpeningBalance(BigDecimal.ZERO);
                vo.setCurrentPayable(BigDecimal.ZERO);
                vo.setCurrentPayment(BigDecimal.ZERO);
                vo.setClosingBalance(BigDecimal.ZERO);
                voList.add(vo);
                continue;
            }

            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentPayable = BigDecimal.ZERO;
            BigDecimal currentPayment = BigDecimal.ZERO;

            LocalDateTime startDateTime = query.getStartDate().atStartOfDay();
            List<Long> supplierIds = jqf.select(qOrderPayment.supplierId).from(qOrderPayment).where(qOrderPayment.id.in(paymentIds)).groupBy(qOrderPayment.supplierId).fetch();

            for (Long supplierId : supplierIds) {
                openingBalance = openingBalance.add(getOpeningBalance(supplierId, startDateTime));
                currentPayable = currentPayable.add(getCurrentPayable(supplierId, query));
                currentPayment = currentPayment.add(getCurrentPayment(supplierId, query));
            }

            BigDecimal closingBalance = openingBalance.add(currentPayable).subtract(currentPayment);

            SummaryPayableDetailsVO vo = new SummaryPayableDetailsVO();
            vo.setSupplierName(staffName);
            vo.setSupplierCode(String.valueOf(staffId));
            vo.setOpeningBalance(openingBalance);
            vo.setCurrentPayable(currentPayable);
            vo.setCurrentPayment(currentPayment);
            vo.setClosingBalance(closingBalance);

            voList.add(vo);
        }

        SummaryPayableDetailsPageVO result = new SummaryPayableDetailsPageVO();
        result.setPayableDetailsList(voList);
        result.setPayableDetailsListTotal(total);
        result.setTotalOpeningBalance(voList.stream().map(SummaryPayableDetailsVO::getOpeningBalance).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalCurrentPayable(voList.stream().map(SummaryPayableDetailsVO::getCurrentPayable).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalCurrentPayment(voList.stream().map(SummaryPayableDetailsVO::getCurrentPayment).reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalClosingBalance(voList.stream().map(SummaryPayableDetailsVO::getClosingBalance).reduce(BigDecimal.ZERO, BigDecimal::add));

        return result;
    }

    public PageResults<PayableDetailReportVO> payableDetail(Page page, PayableDetailReportQuery query) {
        List<PayableDetailReportVO> result = jqf.select(Projections.bean(PayableDetailReportVO.class, qOrderPayment.supplierName.as("supplierName"), qOrderPayment.orderStaffName.as("staffName"), qOrderPayment.orderDate.as("orderDate"), qOrderPayment.orderNo.as("orderNo"), Expressions.cases().when(qItem.id.isNull()).then("预付款").otherwise("采购付款").as("businessType"), qItem.currentVerifyAmount.as("payableAmount"), Expressions.numberTemplate(BigDecimal.class, "CASE WHEN {0} IS NULL THEN {1} ELSE {2} END", qItem.id, qOrderPayment.advanceCollectionsAmount, BigDecimal.ZERO).as("prepaymentAmount"), qOrderPayment.notVerificationAmount.as("balance"), qOrderPayment.remarks.as("remarks"))).from(qOrderPayment).leftJoin(qItem).on(qItem.paymentId.eq(qOrderPayment.id)).where(query.builder, qOrderPayment.orderStatus.eq(OrderStatus.已审核)).offset(page.getOffset()).limit(page.getPageSize()).fetch();

        Long total = jqf.select(qOrderPayment.count()).from(qOrderPayment).leftJoin(qItem).on(qItem.paymentId.eq(qOrderPayment.id)).where(query.builder, qOrderPayment.orderStatus.eq(OrderStatus.已审核)).fetchOne();

        return new PageResults<>(result, page, total == null ? 0 : total);
    }

    private BigDecimal getOpeningBalance(Long supplierId, LocalDateTime dateTime) {
        QOrderPayment qPayment = QOrderPayment.orderPayment;
        QPurchaseOrder qPurchase = QPurchaseOrder.purchaseOrder     ;
        QOrderPaymentItem qItem = QOrderPaymentItem.orderPaymentItem;

        BigDecimal paymentSum = jqf.select(qPayment.collectionAmount.sum()).from(qPayment).where(qPayment.supplierId.eq(supplierId).and(qPayment.approvedAt.lt(dateTime)).and(qPayment.orderStatus.eq(OrderStatus.已审核))).fetchOne();

        BigDecimal purchaseSum = jqf.select(qPurchase.finalAmount.sum()).from(qPurchase).where(qPurchase.supplierId.eq(supplierId).and(qPurchase.approvedAt.lt(dateTime)).and(qPurchase.orderStatus.eq(OrderStatus.已审核))).fetchOne();

        paymentSum = paymentSum == null ? BigDecimal.ZERO : paymentSum;
        purchaseSum = purchaseSum == null ? BigDecimal.ZERO : purchaseSum;
        return purchaseSum.subtract(paymentSum);
    }

    private BigDecimal getCurrentPayable(Long supplierId, SummaryPayableDetailsQuery query) {
        LocalDateTime startTime = query.getStartDate().atStartOfDay();
        LocalDateTime endTime = query.getEndDate().atStartOfDay();
        QPurchaseOrder qPurchase = QPurchaseOrder.purchaseOrder;
        QOrderPayment qPayment = QOrderPayment.orderPayment;
        QOrderPaymentItem qItem = QOrderPaymentItem.orderPaymentItem;
        QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
        QVerification qVerification = QVerification.verification;
        BooleanBuilder scope = new BooleanBuilder();
        if (query.getMerchantId() != null) {
            scope.and(qPurchase.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            scope.and(qPurchase.accountBookId.eq(query.getAccountBookId()));
        }
        // 采购订单应付金额
        BigDecimal purchaseAmount = jqf.select(qPurchase.finalAmount.sum())
                .from(qPurchase)
                .where(qPurchase.supplierId.eq(supplierId)
                        .and(scope)
                        .and(qPurchase.approvedAt.between(startTime, endTime))
                        .and(qPurchase.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        // 其他支出单金额（付款金额 + 欠款金额）
        BooleanBuilder otherScope = new BooleanBuilder()
                .and(qOtherExpense.supplierId.eq(supplierId))
                .and(qOtherExpense.approvedAt.between(startTime, endTime))
                .and(qOtherExpense.orderStatus.eq(OrderStatus.已审核));
        if (query.getMerchantId() != null) {
            otherScope.and(qOtherExpense.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            otherScope.and(qOtherExpense.accountBookId.eq(query.getAccountBookId()));
        }
        BigDecimal otherCollected = jqf.select(qOtherExpense.collectionAmount.sum())
                .from(qOtherExpense)
                .where(otherScope)
                .fetchOne();
        BigDecimal otherArrears = jqf.select(qOtherExpense.arrearsAmount.sum())
                .from(qOtherExpense)
                .where(otherScope)
                .fetchOne();
        BigDecimal otherExpenseAmount = nz(otherCollected).add(nz(otherArrears));

        // 付款单中的折扣金额
        BigDecimal discountAmount = jqf.select(qPayment.discountAmount.sum())
                .from(qPayment)
                .where(qPayment.supplierId.eq(supplierId)
                        .and(qPayment.approvedAt.between(startTime, endTime))
                        .and(qPayment.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        return nz(purchaseAmount).add(otherExpenseAmount).subtract(nz(discountAmount));
    }

    private BigDecimal getCurrentPayment(Long supplierId, SummaryPayableDetailsQuery query) {
        LocalDateTime startTime = query.getStartDate().atStartOfDay();
        LocalDateTime endTime = query.getEndDate().atStartOfDay();
        QOrderPayment qPayment = QOrderPayment.orderPayment;

        BooleanBuilder paymentScope = new BooleanBuilder()
                .and(qPayment.supplierId.eq(supplierId))
                .and(qPayment.approvedAt.between(startTime, endTime))
                .and(qPayment.orderStatus.eq(OrderStatus.已审核));
        BooleanBuilder otherScope = new BooleanBuilder()
                .and(qOtherExpense.supplierId.eq(supplierId))
                .and(qOtherExpense.approvedAt.between(startTime, endTime))
                .and(qOtherExpense.orderStatus.eq(OrderStatus.已审核));
        if (query.getMerchantId() != null) {
            paymentScope.and(qPayment.merchantId.eq(query.getMerchantId()));
            otherScope.and(qOtherExpense.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            paymentScope.and(qPayment.accountBookId.eq(query.getAccountBookId()));
            otherScope.and(qOtherExpense.accountBookId.eq(query.getAccountBookId()));
        }

        BigDecimal orderPayment = jqf.select(qPayment.collectionAmount.sum()).from(qPayment).where(paymentScope).fetchOne();
        BigDecimal otherExpensePayment = jqf.select(qOtherExpense.collectionAmount.sum()).from(qOtherExpense).where(otherScope).fetchOne();

        return nz(orderPayment).add(nz(otherExpensePayment));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public PageResults<OrderPayment> query(Page page, OrderPaymentService.Query query) {
        PagedList<OrderPayment> fetchPage = bqf.selectFrom(qOrderPayment).where(query.builder).orderBy(qOrderPayment.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OrderPayment> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OrderPayment orderPayment = BeanUtil.toBean(tuple, OrderPayment.class);
            dtos.add(orderPayment);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OrderPayment save(OrderPaymentForm dto) {
        if (dto.getOrderPayment() == null) {
            throw new ServiceException("参数错误");
        }
        if (dto.getCollectionList() == null) {
            throw new ServiceException("参数错误");
        }
        OrderPayment orderPayment = dto.getOrderPayment();
        checkoutService.assertEditable(orderPayment.getMerchantId(), orderPayment.getAccountBookId(), orderPayment.getOrderDate());
        if (orderPayment.getId() != null) {
            jqf.delete(QorderPaymentItem).where(QorderPaymentItem.paymentId.eq(orderPayment.getId())).execute();
            jqf.delete(QorderPaymentCollection).where(QorderPaymentCollection.paymentId.eq(Math.toIntExact(orderPayment.getId()))).execute();
        }
        List<OrderPaymentItem> items = dto.getItemList();
        List<OrderPaymentCollection> collections = dto.getCollectionList();
        validatePaymentVerificationRules(orderPayment, items);
        if (CollectionUtils.isEmpty(items)) {
            orderPayment.setOrderType(2);
        } else {
            orderPayment.setOrderType(1);
        }

        if (orderPayment.getOrderStatus() == null) {
            throw new ServiceException("状态为空");
        }

        if (OrderStatus.已审核.equals(orderPayment.getOrderStatus())) {
            if (orderPayment.getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            orderPayment.setApprovedAt(LocalDateTime.now());
        }

        if (orderPayment.getId() == null) {
            orderPayment.setCreatedAt(LocalDateTime.now());
        }

        BigDecimal totalDocumentAmount = BigDecimal.ZERO;
        BigDecimal totalVerifiedAmount = BigDecimal.ZERO;
        BigDecimal totalCurrentVerifyAmount = BigDecimal.ZERO;

        if (items != null && !items.isEmpty()) {
            for (OrderPaymentItem item : items) {
                totalDocumentAmount = totalDocumentAmount.add(item.getDocumentAmount() == null ? BigDecimal.ZERO : item.getDocumentAmount());
                totalVerifiedAmount = totalVerifiedAmount.add(item.getVerifiedAmount() == null ? BigDecimal.ZERO : item.getVerifiedAmount());
                totalCurrentVerifyAmount = totalCurrentVerifyAmount.add(item.getCurrentVerifyAmount() == null ? BigDecimal.ZERO : item.getCurrentVerifyAmount());
            }
        }

        BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        if (!collections.isEmpty()) {
            for (OrderPaymentCollection collection : collections) {
                totalPaymentAmount = totalPaymentAmount.add(collection.getAmount() == null ? BigDecimal.ZERO : collection.getAmount());
            }
        }

        if (orderPayment.getDiscountAmount() != null && orderPayment.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("折扣金额不能为负数");
        }

        BigDecimal discountAmount = orderPayment.getDiscountAmount() == null ? BigDecimal.ZERO : orderPayment.getDiscountAmount();

        // 应核销金额 = 订单总额 + 折扣（取自源单金额，不是实付金额）
        BigDecimal shouldVerifyAmount = totalDocumentAmount.add(discountAmount);
        // 预付款金额 = 实付金额 - 应核销金额（多付的部分）
        BigDecimal advanceAmount = totalPaymentAmount.subtract(shouldVerifyAmount);

        orderPayment.setCollectionAmount(totalPaymentAmount);
        orderPayment.setDiscountAmount(discountAmount);
        orderPayment.setShouldVerificationAmount(shouldVerifyAmount);
        orderPayment.setHasVerificationAmount(totalCurrentVerifyAmount); // 已核销金额（本单）
        orderPayment.setVerificationAmount(totalCurrentVerifyAmount);
        // 未核销金额 = 应核销 - 本单已核销
        BigDecimal notVerifyAmount = shouldVerifyAmount.subtract(totalCurrentVerifyAmount);
        orderPayment.setNotVerificationAmount(notVerifyAmount);
        // 预付款金额 = 实付金额 - 本次核销
        orderPayment.setAdvanceCollectionsAmount(totalPaymentAmount.subtract(totalCurrentVerifyAmount));

        writeOffStatus(items, orderPayment);

        // 更新供应商余额
        updateSupplierBalance(orderPayment);

        if (orderPayment.getId() == null) {
            assignOrderNumber(orderPayment);
            orderPayment.setUpdatedAt(LocalDateTime.now());
            if (orderPayment.getOrderStatus() == null) {
                orderPayment.setOrderStatus(OrderStatus.已保存);
            }
            orderPayment = orderPaymentRepository.save(orderPayment);
            saveItems(orderPayment, items);
            saveCollections(orderPayment, collections);
            // 直接保存为已审核时，同步更新采购入库单核销金额、供应商余额、结算单
            if (OrderStatus.已审核.equals(orderPayment.getOrderStatus()) && items != null) {
                updateSupplierAndAccountBalances(orderPayment, OrderStatus.已审核);
                for (OrderPaymentItem item : items) {
                    settlementService.updateWriteOff(item.getBusinessId(), item.getCurrentVerifyAmount(),
                            orderPayment.getMerchantId(), orderPayment.getAccountBookId(), "采购入库单");
                    if (item.getBusinessId() != null && item.getCurrentVerifyAmount() != null) {
                        jqf.update(qPurchaseOrderInbound)
                                .set(qPurchaseOrderInbound.verifiedAmount,
                                        Expressions.numberTemplate(BigDecimal.class,
                                                "COALESCE({0}, 0) + {1}",
                                                qPurchaseOrderInbound.verifiedAmount,
                                                item.getCurrentVerifyAmount()))
                                .where(qPurchaseOrderInbound.id.eq(item.getBusinessId()))
                                .execute();
                    }
                }
            }
            return orderPayment;
        } else {
            OrderPayment original = orderPaymentRepository.getById(orderPayment.getId());
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            BeanUtil.copyProperties(orderPayment, original, CopyOptions.create().ignoreNullValue());
            saveItems(orderPayment, items);
            saveCollections(orderPayment, collections);
            return orderPaymentRepository.save(original);
        }

    }

    private void saveItems(OrderPayment orderPayment, List<OrderPaymentItem> items) {
        if (items != null && !items.isEmpty()) {
            for (OrderPaymentItem item : items) {
                item.setPaymentId(orderPayment.getId());
                item.setMerchantId(orderPayment.getMerchantId());
                item.setAccountBookId(orderPayment.getAccountBookId());
                orderPaymentItemRepository.save(item);
            }
        }
    }

    private void saveCollections(OrderPayment orderPayment, List<OrderPaymentCollection> collections) {
        if (collections != null && !collections.isEmpty()) {
            for (OrderPaymentCollection collection : collections) {
                collection.setPaymentId(Math.toIntExact(orderPayment.getId()));
                orderPaymentCollectionRepository.save(collection);
            }
        }
    }

    private void assignOrderNumber(OrderPayment orderPayment) {
        if (StrUtil.isEmpty(orderPayment.getOrderNo())) {
            CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(CodeRule.DocumentType.付款单, orderPayment.getMerchantId(), orderPayment.getAccountBookId());

            if (codeRule != null) {
                StringBuilder codeBuilder = new StringBuilder();
                if (StrUtil.isNotBlank(codeRule.getPrefix())) {
                    codeBuilder.append(codeRule.getPrefix());
                }
                if (StrUtil.isNotBlank(codeRule.getFormat())) {
                    String formattedDate = DateUtil.format(LocalDateTime.now(), codeRule.getFormat());
                    codeBuilder.append(formattedDate);
                }
                Integer serialLength = codeRule.getSerialNumberLength();
                if (serialLength != null && serialLength > 0) {
                    JPAQuery<Long> query = jqf.select(qOrderPayment.id.count()).from(qOrderPayment).where(qOrderPayment.merchantId.eq(orderPayment.getMerchantId()).and(qOrderPayment.accountBookId.eq(orderPayment.getAccountBookId())));
                    Long count = query.fetchOne();
                    Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                    String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                    codeBuilder.append(serialStr);
                }
                orderPayment.setOrderNo(codeBuilder.toString());
            } else {
                orderPayment.setOrderNo(CodeGenerator.generateCode());
            }
        }
    }

    @Transactional
    public void updateSupplierBalance(OrderPayment orderPayment) {
        if (orderPayment.getId() != null && OrderStatus.已审核.equals(orderPayment.getOrderStatus())) {
            if (orderPayment.getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            orderPayment.setApprovedAt(LocalDateTime.now());
            updateSupplierAndAccountBalances(orderPayment, OrderStatus.已审核);
        }
    }

    private static void writeOffStatus(List<OrderPaymentItem> items, OrderPayment orderPayment) {
        if (CollectionUtils.isEmpty(items)) {
            orderPayment.setWriteOffStatus(0); // 无明细 → 未核销
        } else if (orderPayment.getHasVerificationAmount().compareTo(BigDecimal.ZERO) <= 0) {
            orderPayment.setWriteOffStatus(0); // 无核销记录
        } else if (orderPayment.getHasVerificationAmount().compareTo(orderPayment.getShouldVerificationAmount()) >= 0) {
            orderPayment.setWriteOffStatus(2); // 全部核销
        } else {
            orderPayment.setWriteOffStatus(1); // 部分核销
        }
    }

    @Transactional
    public void delete(String ids, Long merchantId, Long accountBookId) {
        if (StrUtil.isBlank(ids)) {
            throw new ServiceException("请选择要删除的数据");
        }

        List<Long> idList = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();

        if (idList.isEmpty()) {
            throw new ServiceException("无效的ID列表");
        }
        List<OrderPayment> orderPayments = jqf.select(qOrderPayment).from(qOrderPayment).where(qOrderPayment.id.in(idList).and(qOrderPayment.merchantId.eq(merchantId)).and(qOrderPayment.accountBookId.eq(accountBookId))).fetch();

        if (orderPayments.isEmpty()) {
            throw new ServiceException("没有找到可删除的付款单");
        }
        for (OrderPayment payment : orderPayments) {
            if (!OrderStatus.已保存.equals(payment.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的付款单：" + payment.getOrderNo());
            }
        }
        jqf.delete(QorderPaymentItem).where(QorderPaymentItem.paymentId.in(idList).and(QorderPaymentItem.merchantId.eq(merchantId)).and(QorderPaymentItem.accountBookId.eq(accountBookId))).execute();
        jqf.delete(QorderPaymentCollection).where(QorderPaymentCollection.paymentId.in(idList.stream().map(Math::toIntExact).toList())).execute();

        jqf.delete(qOrderPayment).where(qOrderPayment.id.in(idList).and(qOrderPayment.merchantId.eq(merchantId)).and(qOrderPayment.accountBookId.eq(accountBookId))).execute();
    }

    public PageResults<OrderPaymentQueryVO> query(OrderPaymentService.Query query, Page page) {
        JPAQuery<OrderPayment> mainQuery = jqf.select(qOrderPayment).from(qOrderPayment).leftJoin(qSupplier).on(qSupplier.id.eq(qOrderPayment.supplierId)).where(query.builder).orderBy(qOrderPayment.id.desc());

        List<OrderPayment> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        long total = mainQuery.fetchCount();

        List<OrderPaymentQueryVO> voList = new ArrayList<>();

        for (OrderPayment payment : mainList) {
            OrderPaymentQueryVO vo = BeanUtil.toBean(payment, OrderPaymentQueryVO.class);
            List<OrderPaymentCollection> paymentMethods = jqf.select(Projections.bean(OrderPaymentCollection.class, qCollection.id, qCollection.paymentId, qCollection.settlementAccount, qCollection.paymentMethodId, qCollection.amount, qCollection.remarks, qCollection.paymentMethodName, qCollection.theOnlineTransactionNumber)).from(qCollection).where(qCollection.paymentId.eq(Math.toIntExact(payment.getId()))).fetch().stream().distinct().toList();

            vo.setCollectionList(paymentMethods);
            List<OrderPaymentItem> items = jqf.select(qItem).from(qItem).where(qItem.paymentId.eq(payment.getId())).fetch().stream().distinct().toList();
            vo.setItemList(items);
            voList.add(vo);
        }

        return new PageResults<>(voList, page, total);
    }

    /**
     * 校验付款单明细是否符合核销规则
     */
    private void validatePaymentVerificationRules(OrderPayment orderPayment, List<OrderPaymentItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        Long merchantId = orderPayment.getMerchantId();
        Long accountBookId = orderPayment.getAccountBookId();
        Long supplierId = orderPayment.getSupplierId();

        boolean boo = true;
        Set<Long> purchaseOrderIdSet = new HashSet<>();

        for (OrderPaymentItem item : items) {
            // 当前核销金额不能大于剩余未核销金额
            BigDecimal currentVerifyAmount = item.getCurrentVerifyAmount();
            if (currentVerifyAmount == null || currentVerifyAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("核销金额不能为负数或空：");
            }
            Long purchaseOrderId = item.getBusinessId();
            if (purchaseOrderId == null) {
                throw new ServiceException("列ID为空");
            }
            if (purchaseOrderIdSet.contains(purchaseOrderId)) {
                throw new ServiceException("不能重复引用采购单：" + purchaseOrderId);
            }
            if (purchaseOrderId == -1) {
                boo = false;
                Supplier customer = supplierService.selectByPrimaryKey(orderPayment.getSupplierId());
                if (customer == null) {
                    throw new ServiceException("客户不存在");
                }
                BigDecimal availableAdvance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;
                if (availableAdvance.compareTo(currentVerifyAmount) < 0) {
                    throw new ServiceException("供应商余额不足，无法进行核销");
                }
            } else {

                purchaseOrderIdSet.add(purchaseOrderId);
                PurchaseInbound purchaseOrder = quantityRepository.findById(purchaseOrderId).orElseThrow(() -> new ServiceException("采购单不存在：" + purchaseOrderId));

                if (!OrderStatus.已审核.equals(purchaseOrder.getOrderStatus())) {
                    throw new ServiceException("采购单未审核，无法引用：" + purchaseOrderId);
                }

                if (!purchaseOrder.getSupplierId().equals(supplierId)) {
                    throw new ServiceException("采购单供应商不一致，无法引用：" + purchaseOrderId);
                }

                BigDecimal verifiedAmount = jqf.select(qItem.currentVerifyAmount.sum()).from(qItem).where(qItem.businessId.eq(purchaseOrderId)).fetchOne();

                if (verifiedAmount == null) {
                    verifiedAmount = BigDecimal.ZERO;
                }

                BigDecimal documentAmount = purchaseOrder.getFinalAmount();
                BigDecimal unverifiedAmount = documentAmount.subtract(verifiedAmount);
                if (unverifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("采购单已全部核销，无法再次引用：" + purchaseOrderId);
                }

                if (currentVerifyAmount.compareTo(unverifiedAmount) > 0) {
                    throw new ServiceException("核销金额超过采购单剩余未核销金额：" + purchaseOrderId);
                }

                // 检查是否有单据重复引用
                BigDecimal totalUsedInOtherPayments = jqf.select(qItem.currentVerifyAmount.sum()).from(qItem).leftJoin(qOrderPayment).on(qOrderPayment.id.eq(qItem.paymentId)).where(qItem.businessId.eq(purchaseOrderId).and(qOrderPayment.orderStatus.eq(OrderStatus.已审核)).and(qOrderPayment.merchantId.eq(merchantId)).and(qOrderPayment.accountBookId.eq(accountBookId))).fetchOne();

                if (totalUsedInOtherPayments == null) {
                    totalUsedInOtherPayments = BigDecimal.ZERO;
                }

                // alreadyUsed = 其他已审核付款单对该采购单已使用的核销金额
                // verifiedAmount 已在前面按单明细校验过(第726-733行)，不应重复累加
                BigDecimal alreadyUsed = totalUsedInOtherPayments;
                BigDecimal maxAllowed = documentAmount;

                if (alreadyUsed.add(currentVerifyAmount).compareTo(maxAllowed) > 0) {
                    throw new ServiceException("与其他已审核单据冲突，核销金额将超出采购单总额：" + purchaseOrderId);
                }
            }
        }
        if (boo) {
            BigDecimal totalCurrentVerifyAmount = items.stream().map(OrderPaymentItem::getCurrentVerifyAmount).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDocumentAmount = purchaseOrderIdSet.stream().map(purchaseOrderId -> quantityRepository.findById(purchaseOrderId).get().getFinalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalCurrentVerifyAmount.compareTo(totalDocumentAmount) > 0) {
                throw new ServiceException("所有引用采购单的核销金额总和不能超过总应付金额");
            }
        }
    }

    public OrderPaymentDetails load(Long merchantId, Long id) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        OrderPaymentDetailsVO orderPayment = jqf.select(Projections.bean(OrderPaymentDetailsVO.class, qOrderPayment.id, qOrderPayment.supplierId, qOrderPayment.supplierName, qOrderPayment.orderType, qOrderPayment.orderDate, qOrderPayment.orderNo, qOrderPayment.documentSource, qOrderPayment.discountAmount, qOrderPayment.collectionAmount, qOrderPayment.totalAmountsOwed, qOrderPayment.verificationAmount, qOrderPayment.advanceCollectionsAmount, qOrderPayment.shouldVerificationAmount, qOrderPayment.hasVerificationAmount, qOrderPayment.notVerificationAmount, qOrderPayment.writeOffStatus, qOrderPayment.orderStatus, qOrderPayment.orderStaffId, qOrderPayment.orderStaffName, qOrderPayment.createdBy, qOrderPayment.updatedBy, qOrderPayment.createdAt, qOrderPayment.updatedAt, qOrderPayment.approvedBy, qOrderPayment.approvedAt, qOrderPayment.accountBookId, qOrderPayment.merchantId, qCreatedByUser.name.as("createName"), qUpdatedByUser.name.as("updateName"), qApprovedByUser.name.as("approvedName"))).from(qOrderPayment).leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOrderPayment.createdBy)).leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qOrderPayment.updatedBy)).leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOrderPayment.approvedBy)).where(qOrderPayment.merchantId.eq(merchantId).and(qOrderPayment.id.eq(id))).fetchOne();

        if (orderPayment == null) {
            throw new ServiceException("付款单不存在");
        }

        OrderPaymentDetails dto = new OrderPaymentDetails();
        dto.setOrderPayment(orderPayment);

        List<OrderPaymentCollection> collectionList = jqf.select(qCollection).from(qCollection).where(qCollection.paymentId.eq(Math.toIntExact(orderPayment.getId()))).fetch().stream().distinct().toList();

        dto.setCollectionList(collectionList);

        List<OrderPaymentItem> itemList = jqf.select(qItem).from(qItem).where(qItem.paymentId.eq(orderPayment.getId())).fetch().stream().distinct().toList();

        dto.setItemList(itemList);

        return dto;
    }

    @Transactional
    public void approved(List<Long> ids, OrderStatus state, Long adminId, Long merchantId) {
        OrderPaymentUpdateDTO dto = new OrderPaymentUpdateDTO();
        dto.setId(ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")));
        dto.setOrderStatus(state);
        dto.setApprovedBy(adminId);
        updateStatus(dto);
    }

    @Transactional
    public void updateStatus(OrderPaymentUpdateDTO orderPayment) {
        String ids = orderPayment.getId();
        OrderStatus targetStatus = orderPayment.getOrderStatus();
        if (targetStatus == null) {
            throw new ServiceException("请选择要操作的状态");
        }
        if (orderPayment.getApprovedBy() == null) {
            throw new ServiceException("请选择审核人");
        }
        if (StrUtil.isBlank(ids)) {
            throw new ServiceException("请选择要操作的数据");
        }
        List<Long> idList = Arrays.stream(ids.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(Long::valueOf).toList();

        if (idList.isEmpty()) {
            throw new ServiceException("无效的ID列表");
        }
        List<OrderPayment> payments = jqf.select(qOrderPayment).from(qOrderPayment).where(qOrderPayment.id.in(idList)).fetch();

        if (payments.size() != idList.size()) {
            throw new ServiceException("存在付款单不存在，请检查数据");
        }
        for (OrderPayment payment : payments) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(payment.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + payment.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(payment.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + payment.getOrderNo());
            }
        }

        LocalDateTime now = LocalDateTime.now();
        jqf.update(qOrderPayment).set(qOrderPayment.orderStatus, targetStatus).set(qOrderPayment.approvedAt, targetStatus == OrderStatus.已审核 ? now : null).set(qOrderPayment.approvedBy, targetStatus == OrderStatus.已审核 ? orderPayment.getApprovedBy() : null).where(qOrderPayment.id.in(idList)).execute();

        for (OrderPayment payment : payments) {
            updateSupplierAndAccountBalances(payment, targetStatus);
            // 审核时同步更新结算单
            if (targetStatus == OrderStatus.已审核) {
                List<OrderPaymentItem> items = jqf.select(qItem).from(qItem)
                        .where(qItem.paymentId.eq(payment.getId())).fetch();
                for (OrderPaymentItem item : items) {
                    settlementService.updateWriteOff(item.getBusinessId(), item.getCurrentVerifyAmount(),
                            payment.getMerchantId(), payment.getAccountBookId(), "采购入库单");
                    // 同步更新采购入库单的已核销金额
                    if (item.getBusinessId() != null && item.getCurrentVerifyAmount() != null) {
                        jqf.update(qPurchaseOrderInbound)
                                .set(qPurchaseOrderInbound.verifiedAmount,
                                        Expressions.numberTemplate(BigDecimal.class,
                                                "COALESCE({0}, 0) + {1}",
                                                qPurchaseOrderInbound.verifiedAmount,
                                                item.getCurrentVerifyAmount()))
                                .where(qPurchaseOrderInbound.id.eq(item.getBusinessId()))
                                .execute();
                    }
                }
            } else if (targetStatus == OrderStatus.已保存) {
                // 反审核时还原已核销金额
                List<OrderPaymentItem> items = jqf.select(qItem).from(qItem)
                        .where(qItem.paymentId.eq(payment.getId())).fetch();
                for (OrderPaymentItem item : items) {
                    if (item.getBusinessId() != null && item.getCurrentVerifyAmount() != null) {
                        // 对称回减关联结算单已核销金额(修复反审核不同步结算单 → 重复累加/残留已平账)
                        settlementService.reverseWriteOff(item.getBusinessId(), item.getCurrentVerifyAmount(),
                                payment.getMerchantId(), payment.getAccountBookId(), "采购入库单");
                        jqf.update(qPurchaseOrderInbound)
                                .set(qPurchaseOrderInbound.verifiedAmount,
                                        Expressions.numberTemplate(BigDecimal.class,
                                                "COALESCE({0}, 0) - {1}",
                                                qPurchaseOrderInbound.verifiedAmount,
                                                item.getCurrentVerifyAmount()))
                                .where(qPurchaseOrderInbound.id.eq(item.getBusinessId()))
                                .execute();
                    }
                }
            }
        }

    }

    @Transactional
    public void updateSupplierAndAccountBalances(OrderPayment payment, OrderStatus targetStatus) {

        if (targetStatus == OrderStatus.已保存 && payment.getOrderStatus() == OrderStatus.已审核) {
            Boolean exists = jqf.select(qVerificationItem.id.isNotNull()).from(qVerificationItem).leftJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId)).where(qVerificationItem.businessId.eq(payment.getId().intValue()).and(qVerificationItem.businessType.eq(1)).and(qVerification.type.eq(2))
//                                    .and(qVerification.orderStatus.eq(OrderStatus.已审核))
            ).fetchFirst() != null;

            if (exists) {
                throw new ServiceException("该付款单已被核销单引用，无法进行反审核");
            }
        }

        Supplier supplier = supplierService.selectByPrimaryKey(payment.getSupplierId());

        BigDecimal shouldVerifyAmount = payment.getShouldVerificationAmount();
        if (shouldVerifyAmount == null || shouldVerifyAmount.compareTo(BigDecimal.ZERO) < 0) {
            shouldVerifyAmount = BigDecimal.ZERO;
        }

        if (targetStatus == OrderStatus.已审核) {
            supplier.setBalance(supplier.getBalance().subtract(shouldVerifyAmount));
        } else {
            if (payment.getOrderStatus() != OrderStatus.已审核) {
                throw new ServiceException("只有已审核的付款单才能反审核");
            }
            supplier.setBalance(supplier.getBalance().add(shouldVerifyAmount));

        }
        SupplierFlow supplierFlow = getSupplierFlow(payment, targetStatus, supplier);
        supplierService.updateTheBalance(supplier, supplierFlow);
        List<OrderPaymentCollection> collections = jqf.select(qCollection).from(qCollection).where(qCollection.paymentId.eq(Math.toIntExact(payment.getId()))).fetch().stream().distinct().toList();
        if (collections.isEmpty()) {
            throw new ServiceException("未找到结算账户信息");
        }

        for (OrderPaymentCollection collection : collections) {
            Long accountId = collection.getSettlementAccountId();
            if (accountId == null) {
                throw new ServiceException("结算账户不能为空");
            }
            BigDecimal amount = collection.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("金额必须大于零");
            }

            AccountBalanceChangeContext context = AccountBalanceChangeContext.builder().accountId(accountId).merchantId(payment.getMerchantId()).accountBookId(payment.getAccountBookId()).voucherId(payment.getId()).supplierId(payment.getSupplierId()).correspondentsId(payment.getSupplierId()).correspondentsName(payment.getSupplierName()).businessNo(payment.getOrderNo()).flowType(AccountFlow.AccountFlowType.付款单).operatorId(payment.getOrderStaffId()).operatorName(payment.getOrderStaffName()).remarks(targetStatus == OrderStatus.已审核 ? "付款单审核通过" : "付款单反审核").build();

            if (targetStatus == OrderStatus.已审核) {
                context.setAmount(amount);
            } else {
                context.setAmount(amount.negate());
            }

            accountService.updateAccountBalanceWithFlow(context);
        }
    }

    private static @NotNull SupplierFlow getSupplierFlow(OrderPayment payment, OrderStatus targetStatus, Supplier supplier) {
        SupplierFlow.SupplierFlowType flowType;
        SupplierFlow supplierFlow = new SupplierFlow();
        supplierFlow.setSupplierId(supplier.getId());
        supplierFlow.setBusinessId(payment.getId());
        supplierFlow.setBusinessNo(payment.getOrderNo());
        supplierFlow.setBusinessDate(payment.getOrderDate());
        // 实付金额
        if (targetStatus == OrderStatus.已审核) {
            flowType = SupplierFlow.SupplierFlowType.付款单;
            supplierFlow.setActualPaymentAmount(payment.getCollectionAmount());
        } else {
            flowType = SupplierFlow.SupplierFlowType.反审核_付款单;
            supplierFlow.setActualPaymentAmount(payment.getCollectionAmount() != null ? payment.getCollectionAmount().negate() : BigDecimal.ZERO);
        }
        supplierFlow.setSupplierFlowType(flowType);
        // 应付余额
        supplierFlow.setBalancePayable(supplier.getBalance());
        supplierFlow.setAccountBookId(payment.getAccountBookId());
        supplierFlow.setMerchantId(payment.getMerchantId());
        supplierFlow.setCreatedBy(payment.getApprovedBy());
        supplierFlow.setCreatedAt(LocalDateTime.now());
        supplierFlow.setRemarks(targetStatus == OrderStatus.已审核 ? "付款单审核通过" : "付款单反审核");
        return supplierFlow;
    }

    public Object writeOffCandidates(Page page, SupplierQuery query) {
        if (query.getSupplierId() == null) {
            throw new ServiceException("供应商ID不能为空");
        }

        QPurchaseInbound qPurchaseInbound = QPurchaseInbound.purchaseInbound;

        NumberExpression<BigDecimal> paymentVerifySum = qOrderPaymentItem.currentVerifyAmount.sum().
                coalesce(BigDecimal.ZERO);

        NumberExpression<BigDecimal> verificationVerifySum = Expressions.numberTemplate(BigDecimal.class,
                "COALESCE(SUM(CASE WHEN {0} IS NOT NULL THEN {1} ELSE 0 END), 0)",
                qVerification.id, qVerificationItem.currentVerifyAmount).coalesce(BigDecimal.ZERO);

        NumberExpression<BigDecimal> totalVerifiedExpr = paymentVerifySum.add(verificationVerifySum);
        NumberExpression<BigDecimal> unverifiedExpr = qPurchaseInbound.finalAmount.subtract(totalVerifiedExpr);

        JPAQuery<PurchaseOrderWithVerification> mainQuery = jqf.select(
                Projections.fields(PurchaseOrderWithVerification.class,
                        qPurchaseInbound.id.as("salesOrderId"),
                        qPurchaseInbound.orderNo.as("salesOrderNo"),
                        qPurchaseInbound.inboundDate.as("businessDate"),
                        qPurchaseInbound.finalAmount.as("documentAmount"),
                        totalVerifiedExpr.as("verifiedAmount"),
                        unverifiedExpr.as("unverifiedAmount"))).from(qPurchaseInbound)
                .leftJoin(qOrderPaymentItem).
                on(qOrderPaymentItem.businessId.eq(qPurchaseInbound.id)).leftJoin(qVerificationItem)
                .on(qVerificationItem.businessId.eq(qPurchaseInbound.id.intValue())
                        .and(qVerificationItem.businessType.eq(2))).leftJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId).and(qVerification.orderStatus.eq(OrderStatus.已审核))).where(query.builder.and(qPurchaseInbound.orderStatus.eq(OrderStatus.已审核))).groupBy(qPurchaseInbound.id, qPurchaseInbound.orderNo, qPurchaseInbound.inboundDate, qPurchaseInbound.finalAmount).having(unverifiedExpr.gt(BigDecimal.ZERO));

        List<PurchaseOrderWithVerification> result = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();

        long total = mainQuery.fetchCount();

        return new PageResults<>(result, page, total);
    }

    @Data
    public static class PurchaseOrderWithVerification {
        private Long salesOrderId;
        private String salesOrderNo;
        private Integer businessType = 1;
        private LocalDate businessDate;
        private BigDecimal documentAmount;
        private BigDecimal verifiedAmount;
        private BigDecimal unverifiedAmount;
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qOrderPayment)
                .leftJoin(qSupplier).on(qSupplier.id.eq(qOrderPayment.supplierId))
                .select(qOrderPayment.collectionAmount.sum())
                .where(query.builder).fetchFirst();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setOrderType(Integer orderType) {
            if (orderType != null) {
                builder.and(qOrderPayment.orderType.eq(orderType));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qOrderPayment.merchantId, merchantId);
        }

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qOrderPayment.supplierId.eq(supplierId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qOrderPayment.accountBookId, accountBookId);
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qOrderPayment.orderStatus.eq(orderStatus));
            }
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotEmpty(startTime)) {
                builder.and(qOrderPayment.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotEmpty(endTime)) {
                builder.and(qOrderPayment.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }

        public void setKeyword(String keyword) {
            if (StrUtil.isNotBlank(keyword)) {
                builder.and(qOrderPayment.orderNo.like("%" + keyword + "%").or(qSupplier.name.like("%" + keyword + "%")));
            }
        }

        public void setWriteOff(Integer writeOff) {
            if (writeOff != null && writeOff == 1) {
                builder.and(qOrderPayment.notVerificationAmount.gt(BigDecimal.ZERO));
            }
        }

        public void setAdvance(Integer advance) {
            if (advance != null && advance == 1) {
                builder.and(qOrderPayment.advanceCollectionsAmount.gt(BigDecimal.ZERO));
            }
        }

    }

    public static class PayableDetailReportQuery implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qOrderPayment.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qOrderPayment.accountBookId, accountBookId);
        }

        public void setOrderType(Integer orderType) {
            if (orderType != null) {
                builder.and(qOrderPayment.orderType.eq(orderType));
            }
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qOrderPayment.orderStatus.eq(orderStatus));
            }
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotEmpty(startTime)) {
                builder.and(qOrderPayment.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotEmpty(endTime)) {
                builder.and(qOrderPayment.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }

        public void setKeyword(String keyword) {
            if (StrUtil.isNotBlank(keyword)) {
                builder.and(qOrderPayment.orderNo.like("%" + keyword + "%").or(qOrderPayment.supplierName.like("%" + keyword + "%")));
            }
        }

        public void setSupplierId(Long supplierId) {
            if (supplierId != null) {
                builder.and(qOrderPayment.supplierId.eq(supplierId));
            }
        }
    }

    @Data
    public static class SummaryPayableDetailsQuery implements TenantAware {
        private Long merchantId;
        private Long accountBookId;
        private Long supplierId;
        private Long supplierTypeId;
        private Integer salesmanId;
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer type; //1=按供应商，2=按供应商类型，3=按业务员
    }

    public static class SupplierQuery implements TenantAware {
        @Getter
        Long supplierId;

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPurchaseOrderInbound.merchantId, merchantId);
        }

        public void setOrderNo(String orderNo) {
            if (orderNo != null) {
                builder.and(qPurchaseOrderInbound.orderNo.like("%" + orderNo + "%"));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPurchaseOrderInbound.accountBookId, accountBookId);
        }

        public void setSupplierId(Long supplierId) {
            this.supplierId = supplierId;
            if (supplierId != null) {
                builder.and(qPurchaseOrderInbound.supplierId.eq(supplierId));
            }

        }
    }

}
