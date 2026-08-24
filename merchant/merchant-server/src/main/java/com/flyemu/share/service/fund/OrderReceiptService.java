package com.flyemu.share.service.fund;

import com.flyemu.share.repository.fund.OrderReceiptCollectionRepository;
import com.flyemu.share.repository.fund.OrderReceiptItemRepository;
import com.flyemu.share.repository.fund.OrderReceiptRepository;
import com.flyemu.share.repository.sales.SalesOutboundRepository;
import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.*;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.form.OrderReceiptForm;
import com.flyemu.share.service.fund.vo.*;
import com.flyemu.share.service.fund.vo.report.ReceivableDetailReportVO;
import com.flyemu.share.service.fund.vo.report.SummaryReceivableDetailsPageVO;
import com.flyemu.share.service.fund.vo.report.SummaryReceivableDetailsQuery;
import com.flyemu.share.service.fund.vo.report.SummaryReceivableDetailsVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class OrderReceiptService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QOrderReceipt qOrderReceipt = QOrderReceipt.orderReceipt;
    private final static QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
    private final static QVerification qVerification = QVerification.verification;
    private final static QOrderReceiptItem qItem = QOrderReceiptItem.orderReceiptItem;
    private final QOrderReceiptCollection qCollection = QOrderReceiptCollection.orderReceiptCollection;
    private final QCustomer qCustomer = QCustomer.customer;
    private final QCustomerCategory qCustomerCategory = QCustomerCategory.customerCategory;

    private final static QPaymentMethod qPaymentMethod = QPaymentMethod.paymentMethod;
    private final OrderReceiptRepository orderReceiptRepository;
    private final CodeRuleService codeRuleService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final SalesOutboundRepository salesOrderRepository;
    private final OrderReceiptItemRepository orderReceiptItemRepository;
    private final OrderReceiptCollectionRepository orderReceiptCollectionRepository;
    private final SettlementService settlementService;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final static QOrderReceiptItem qOrderReceiptItem = QOrderReceiptItem.orderReceiptItem;
    private final static QOrderReceiptCollection qOrderReceiptCollection = QOrderReceiptCollection.orderReceiptCollection;
    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;

    public SummaryReceivableDetailsPageVO summaryReceivableDetails(Page page, SummaryReceivableDetailsQuery query) {
        Integer type = query.getType();
        if (type == null || type < 1 || type > 3) {
            throw new ServiceException("type 参数必须为 1、2 或 3");
        }
        return switch (type) {
            case 1 -> handleByCustomer(page, query);
            case 2 -> handleByCustomerCategory(page, query);
            case 3 -> handleByStaff(page, query);
            default -> throw new ServiceException("不支持的查询类型");
        };
    }

    private SummaryReceivableDetailsPageVO handleByCustomer(Page page, SummaryReceivableDetailsQuery query) {
        QCustomer qCustomer = QCustomer.customer;

        BooleanBuilder customerCondition = new BooleanBuilder();
        if (query.getMerchantId() != null) {
            customerCondition.and(qCustomer.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            customerCondition.and(qCustomer.accountBookId.eq(query.getAccountBookId()));
        }
        if (query.getCustomerId() != null) {
            customerCondition.and(qCustomer.id.eq(query.getCustomerId()));
        }

        LocalDateTime startDateTime = query.getStartDate().atStartOfDay();

        JPAQuery<Customer> customerQuery = jqf.selectFrom(qCustomer).where(customerCondition);
        long total = customerQuery.fetchCount();

        List<Customer> customers = customerQuery
                .offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        if (customers.isEmpty()) {
            return new SummaryReceivableDetailsPageVO();
        }

        List<SummaryReceivableDetailsVO> voList = new ArrayList<>();
        SummaryReceivableDetailsPageVO totalVO = new SummaryReceivableDetailsPageVO();
        totalVO.setTotalOpeningBalance(BigDecimal.ZERO);
        totalVO.setTotalCurrentReceivable(BigDecimal.ZERO);
        totalVO.setTotalCurrentReceipt(BigDecimal.ZERO);
        totalVO.setTotalClosingBalance(BigDecimal.ZERO);

        for (Customer customer : customers) {
            SummaryReceivableDetailsVO vo = new SummaryReceivableDetailsVO();
            CustomerCategory byId = jqf.select(qCustomerCategory)
                    .from(qCustomerCategory)
                    .where(qCustomerCategory.id.eq(customer.getCustomerCategoryId()))
                    .fetchFirst();
            if (byId != null) {
                vo.setCustomerCategory(byId.getName());
            }
            vo.setCustomerCode(customer.getCode());
            vo.setCustomerName(customer.getName());

            BigDecimal openingBalance = getOpeningBalance(customer.getId(), startDateTime);
            BigDecimal currentReceivable = getCurrentReceivable(customer.getId(), query);
            BigDecimal currentReceipt = getCurrentReceipt(customer.getId(), query);
            BigDecimal closingBalance = openingBalance.add(currentReceivable).subtract(currentReceipt);

            vo.setOpeningBalance(openingBalance);
            vo.setCurrentReceivable(currentReceivable);
            vo.setCurrentReceipt(currentReceipt);
            vo.setClosingBalance(closingBalance);

            voList.add(vo);

            totalVO.setTotalOpeningBalance(totalVO.getTotalOpeningBalance().add(openingBalance));
            totalVO.setTotalCurrentReceivable(totalVO.getTotalCurrentReceivable().add(currentReceivable));
            totalVO.setTotalCurrentReceipt(totalVO.getTotalCurrentReceipt().add(currentReceipt));
            totalVO.setTotalClosingBalance(totalVO.getTotalClosingBalance().add(closingBalance));
        }

        totalVO.setReceivableDetailsList(voList);
        totalVO.setReceivableDetailsListTotal((int) total);
        return totalVO;
    }

    private SummaryReceivableDetailsPageVO handleByCustomerCategory(Page page, SummaryReceivableDetailsQuery query) {
        QCustomerCategory qCustomerCategory = QCustomerCategory.customerCategory;

        BooleanBuilder condition = new BooleanBuilder();
        if (query.getMerchantId() != null) {
            condition.and(qCustomerCategory.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            condition.and(qCustomerCategory.accountBookId.eq(query.getAccountBookId()));
        }
        if (query.getCustomerTypeId() != null) {
            condition.and(qCustomerCategory.id.eq(query.getCustomerTypeId()));
        }

        JPAQuery<CustomerCategory> categoryQuery = jqf.select(qCustomerCategory)
                .from(qCustomerCategory)
                .where(condition)
                .orderBy(qCustomerCategory.id.asc());

        long total = categoryQuery.fetchCount();

        List<CustomerCategory> categories = categoryQuery
                .offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        List<SummaryReceivableDetailsVO> voList = new ArrayList<>();

        for (CustomerCategory category : categories) {
            String categoryName = category.getName();

            List<Long> customerIds = jqf.select(QCustomer.customer.id)
                    .from(QCustomer.customer)
                    .where(QCustomer.customer.customerCategoryId.eq(category.getId())
                            .and(QCustomer.customer.merchantId.eq(query.getMerchantId()))
                            .and(QCustomer.customer.accountBookId.eq(query.getAccountBookId())))
                    .fetch();

            if (customerIds.isEmpty()) {
                continue;
            }

            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentReceivable = BigDecimal.ZERO;
            BigDecimal currentReceipt = BigDecimal.ZERO;

            LocalDateTime startDateTime = query.getStartDate().atStartOfDay();

            for (Long customerId : customerIds) {
                openingBalance = openingBalance.add(getOpeningBalance(customerId, startDateTime));
                currentReceivable = currentReceivable.add(getCurrentReceivable(customerId, query));
                currentReceipt = currentReceipt.add(getCurrentReceipt(customerId, query));
            }

            BigDecimal closingBalance = openingBalance.add(currentReceivable).subtract(currentReceipt);

            SummaryReceivableDetailsVO vo = new SummaryReceivableDetailsVO();
            vo.setCustomerCategory(categoryName);
            vo.setOpeningBalance(openingBalance);
            vo.setCurrentReceivable(currentReceivable);
            vo.setCurrentReceipt(currentReceipt);
            vo.setClosingBalance(closingBalance);

            voList.add(vo);
        }

        SummaryReceivableDetailsPageVO result = new SummaryReceivableDetailsPageVO();
        result.setTotalOpeningBalance(BigDecimal.ZERO);
        result.setTotalCurrentReceivable(BigDecimal.ZERO);
        result.setTotalCurrentReceipt(BigDecimal.ZERO);
        result.setTotalClosingBalance(BigDecimal.ZERO);

        for (SummaryReceivableDetailsVO vo : voList) {
            result.setTotalOpeningBalance(result.getTotalOpeningBalance().add(vo.getOpeningBalance()));
            result.setTotalCurrentReceivable(result.getTotalCurrentReceivable().add(vo.getCurrentReceivable()));
            result.setTotalCurrentReceipt(result.getTotalCurrentReceipt().add(vo.getCurrentReceipt()));
            result.setTotalClosingBalance(result.getTotalClosingBalance().add(vo.getClosingBalance()));
        }
        result.setReceivableDetailsList(voList);
        result.setReceivableDetailsListTotal(total);
        return result;
    }

    private SummaryReceivableDetailsPageVO handleByStaff(Page page, SummaryReceivableDetailsQuery query) {
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
        List<OrderStaff> staffList = staffQuery
                .offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        List<SummaryReceivableDetailsVO> voList = new ArrayList<>();

        for (OrderStaff staff : staffList) {
            Long staffId = staff.getId().longValue();
            String staffName = staff.getName();

            List<Long> receiptIds = jqf.select(qOrderReceipt.id)
                    .from(qOrderReceipt)
                    .where(qOrderReceipt.orderStaffId.eq(staffId)
                            .and(qOrderReceipt.merchantId.eq(query.getMerchantId()))
                            .and(qOrderReceipt.accountBookId.eq(query.getAccountBookId())))
                    .fetch();

            if (receiptIds.isEmpty()) {
                SummaryReceivableDetailsVO vo = new SummaryReceivableDetailsVO();
                vo.setCustomerName(staffName);
                vo.setCustomerCode(String.valueOf(staffId));
                vo.setOpeningBalance(BigDecimal.ZERO);
                vo.setCurrentReceivable(BigDecimal.ZERO);
                vo.setCurrentReceipt(BigDecimal.ZERO);
                vo.setClosingBalance(BigDecimal.ZERO);
                voList.add(vo);
                continue;
            }

            BigDecimal openingBalance = BigDecimal.ZERO;
            BigDecimal currentReceivable = BigDecimal.ZERO;
            BigDecimal currentReceipt = BigDecimal.ZERO;

            LocalDateTime startDateTime = query.getStartDate().atStartOfDay();
            List<Long> customerIds = jqf.select(qOrderReceipt.customerId)
                    .from(qOrderReceipt)
                    .where(qOrderReceipt.id.in(receiptIds)).groupBy(qOrderReceipt.customerId)
                    .fetch();

            for (Long customerId : customerIds) {
                openingBalance = openingBalance.add(getOpeningBalance(customerId, startDateTime));
                currentReceivable = currentReceivable.add(getCurrentReceivable(customerId, query));
                currentReceipt = currentReceipt.add(getCurrentReceipt(customerId, query));
            }

            BigDecimal closingBalance = openingBalance.add(currentReceivable).subtract(currentReceipt);

            SummaryReceivableDetailsVO vo = new SummaryReceivableDetailsVO();
            vo.setCustomerName(staffName);
            vo.setCustomerCode(String.valueOf(staffId));
            vo.setOpeningBalance(openingBalance);
            vo.setCurrentReceivable(currentReceivable);
            vo.setCurrentReceipt(currentReceipt);
            vo.setClosingBalance(closingBalance);

            voList.add(vo);
        }

        SummaryReceivableDetailsPageVO result = new SummaryReceivableDetailsPageVO();
        result.setReceivableDetailsList(voList);
        result.setReceivableDetailsListTotal(total);
        result.setTotalOpeningBalance(voList.stream()
                .map(SummaryReceivableDetailsVO::getOpeningBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalCurrentReceivable(voList.stream()
                .map(SummaryReceivableDetailsVO::getCurrentReceivable)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalCurrentReceipt(voList.stream()
                .map(SummaryReceivableDetailsVO::getCurrentReceipt)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        result.setTotalClosingBalance(voList.stream()
                .map(SummaryReceivableDetailsVO::getClosingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return result;
    }

    private BigDecimal getOpeningBalance(Long customerId, LocalDateTime dateTime) {
        QOrderReceipt qReceipt = QOrderReceipt.orderReceipt;
        QSalesOutbound qSales = QSalesOutbound.salesOutbound;

        BigDecimal receiptSum = jqf.select(qReceipt.collectionAmount.sum())
                .from(qReceipt)
                .where(qReceipt.customerId.eq(customerId)
                        .and(qReceipt.approvedAt.lt(dateTime))
                        .and(qReceipt.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        BigDecimal salesSum = jqf.select(qSales.finalAmount.sum())
                .from(qSales)
                .where(qSales.customerId.eq(customerId)
                        .and(qSales.approvedAt.lt(dateTime))
                        .and(qSales.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        receiptSum = receiptSum == null ? BigDecimal.ZERO : receiptSum;
        salesSum = salesSum == null ? BigDecimal.ZERO : salesSum;
        return salesSum.subtract(receiptSum);
    }

    private BigDecimal getCurrentReceivable(Long customerId, SummaryReceivableDetailsQuery query) {
        LocalDateTime startTime = query.getStartDate().atStartOfDay();
        LocalDateTime endTime = query.getEndDate().atStartOfDay();
        QSalesOutbound qSales = QSalesOutbound.salesOutbound;
        QOrderReceipt qReceipt = QOrderReceipt.orderReceipt;

        // 销售订单应收金额
        BigDecimal salesAmount = jqf.select(qSales.finalAmount.sum())
                .from(qSales)
                .where(qSales.customerId.eq(customerId)
                        .and(qSales.approvedAt.between(startTime, endTime))
                        .and(qSales.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        // 其他收入金额（收款金额 + 欠款金额）
        QOtherReceipt qOtherReceipt = QOtherReceipt.otherReceipt;
        BooleanBuilder otherBuilder = new BooleanBuilder()
                .and(qOtherReceipt.customerId.eq(customerId))
                .and(qOtherReceipt.approvedAt.between(startTime, endTime))
                .and(qOtherReceipt.orderStatus.eq(OrderStatus.已审核));
        if (query.getMerchantId() != null) {
            otherBuilder.and(qOtherReceipt.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            otherBuilder.and(qOtherReceipt.accountBookId.eq(query.getAccountBookId()));
        }
        BigDecimal otherCollected = jqf.select(qOtherReceipt.collectionAmount.sum())
                .from(qOtherReceipt)
                .where(otherBuilder)
                .fetchOne();
        BigDecimal otherArrears = jqf.select(qOtherReceipt.arrearsAmount.sum())
                .from(qOtherReceipt)
                .where(otherBuilder)
                .fetchOne();
        BigDecimal otherIncomeAmount = nz(otherCollected).add(nz(otherArrears));

        // 收款单中的折扣金额
        BigDecimal discountAmount = jqf.select(qReceipt.discountAmount.sum())
                .from(qReceipt)
                .where(qReceipt.customerId.eq(customerId)
                        .and(qReceipt.approvedAt.between(startTime, endTime))
                        .and(qReceipt.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        salesAmount = nz(salesAmount);
        discountAmount = nz(discountAmount);

        return salesAmount
                .add(otherIncomeAmount)
                .subtract(discountAmount);
    }

    private BigDecimal getCurrentReceipt(Long customerId, SummaryReceivableDetailsQuery query) {
        LocalDateTime startTime = query.getStartDate().atStartOfDay();
        LocalDateTime endTime = query.getEndDate().atStartOfDay();
        QSalesOutbound qSales = QSalesOutbound.salesOutbound;
        QOrderReceipt qReceipt = QOrderReceipt.orderReceipt;

        // 销售订单收款金额
        BigDecimal salesPayment = jqf.select(qSales.finalAmount.sum())
                .from(qSales)
                .where(qSales.customerId.eq(customerId)
                        .and(qSales.approvedAt.between(startTime, endTime))
                        .and(qSales.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        // 收款单收款金额
        BigDecimal orderPayment = jqf.select(qReceipt.collectionAmount.sum())
                .from(qReceipt)
                .where(qReceipt.customerId.eq(customerId)
                        .and(qReceipt.approvedAt.between(startTime, endTime))
                        .and(qReceipt.orderStatus.eq(OrderStatus.已审核)))
                .fetchOne();

        // 其他收入收款金额
        QOtherReceipt qOtherReceipt = QOtherReceipt.otherReceipt;
        BooleanBuilder otherBuilder = new BooleanBuilder()
                .and(qOtherReceipt.customerId.eq(customerId))
                .and(qOtherReceipt.approvedAt.between(startTime, endTime))
                .and(qOtherReceipt.orderStatus.eq(OrderStatus.已审核));
        if (query.getMerchantId() != null) {
            otherBuilder.and(qOtherReceipt.merchantId.eq(query.getMerchantId()));
        }
        if (query.getAccountBookId() != null) {
            otherBuilder.and(qOtherReceipt.accountBookId.eq(query.getAccountBookId()));
        }
        BigDecimal otherIncomePayment = jqf.select(qOtherReceipt.collectionAmount.sum())
                .from(qOtherReceipt)
                .where(otherBuilder)
                .fetchOne();

        return nz(salesPayment)
                .add(nz(orderPayment))
                .add(nz(otherIncomePayment));
    }

    private static BigDecimal nz(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    public PageResults<ReceivableDetailReportVO> receivableDetail(Page page, ReceivableDetailReportQuery query) {
        QOrderReceipt qReceipt = OrderReceiptService.qOrderReceipt;
        QOrderReceiptItem qItem = OrderReceiptService.qItem;

        List<ReceivableDetailReportVO> result = jqf.select(
                        Projections.bean(ReceivableDetailReportVO.class,
                                qReceipt.customerName.as("customerName"),
                                qReceipt.orderDate.as("orderDate"),
                                qReceipt.orderNo.as("orderNo"),
                                Expressions.cases()
                                        .when(qItem.id.isNull()).then("预收款")
                                        .otherwise("销售收款")
                                        .as("businessType"),
                                qItem.currentVerifyAmount.as("receivableAmount"),
                                Expressions.numberTemplate(BigDecimal.class,
                                                "CASE WHEN {0} IS NULL THEN {1} ELSE {2} END",
                                                qItem.id,
                                                qReceipt.advanceCollectionsAmount,
                                                BigDecimal.ZERO)
                                        .as("prepaymentAmount"),
                                qReceipt.notVerificationAmount.as("balance"),
                                qReceipt.orderStaffName.as("staffName"),
                                qReceipt.remarks.as("remarks")
                        )
                )
                .from(qReceipt)
                .leftJoin(qItem).on(qItem.receiptId.eq(qReceipt.id))
                .where(query.builder, qReceipt.orderStatus.eq(OrderStatus.已审核))
                .offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        Long total = jqf.select(qReceipt.count())
                .from(qReceipt)
                .leftJoin(qItem).on(qItem.receiptId.eq(qReceipt.id))
                .where(query.builder, qReceipt.orderStatus.eq(OrderStatus.已审核))
                .fetchOne();

        return new PageResults<>(result, page, total == null ? 0 : total);
    }

    public PageResults<OrderReceiptQueryVO> query(OrderReceiptService.Query query, Page page) {
        JPAQuery<OrderReceipt> mainQuery = jqf.select(qOrderReceipt).from(qOrderReceipt).where(query.builder).orderBy(qOrderReceipt.id.desc());

        List<OrderReceipt> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();

        long total = mainQuery.fetchCount();

        List<OrderReceiptQueryVO> voList = new ArrayList<>();

        for (OrderReceipt receipt : mainList) {
            OrderReceiptQueryVO vo = BeanUtil.toBean(receipt, OrderReceiptQueryVO.class);
            List<OrderReceiptCollectionVO> paymentMethods = jqf.select(Projections.bean(OrderReceiptCollectionVO.class, qCollection.id, qCollection.receiptId, qCollection.settlementAccount, qCollection.paymentMethodId, qCollection.amount, qCollection.remarks, qCollection.theOnlineTransactionNumber, qPaymentMethod.name.as("paymentMethodName"))).from(qCollection).leftJoin(qPaymentMethod).on(qPaymentMethod.id.eq(qCollection.paymentMethodId.longValue())).where(qCollection.receiptId.eq(Math.toIntExact(receipt.getId()))).fetch().stream().distinct().toList();
            vo.setCollectionList(paymentMethods);

            List<OrderReceiptItem> salesOrders = jqf.select(qItem).from(qItem).where(qItem.receiptId.eq(receipt.getId())).fetch().stream().distinct().toList();
            vo.setItemList(salesOrders);
            voList.add(vo);
        }

        return new PageResults<>(voList, page, total);

    }

    @Transactional
    public OrderReceipt save(OrderReceiptForm dto) {
        if (dto.getOrderReceipt() == null) {
            throw new ServiceException("主订单参数错误");
        }
        if (dto.getCollectionList() == null) {
            throw new ServiceException("账户参数错误");
        }
        OrderReceipt orderReceipt = dto.getOrderReceipt();
        checkoutService.assertEditable(orderReceipt.getMerchantId(), orderReceipt.getAccountBookId(), orderReceipt.getOrderDate());
        if (orderReceipt.getId() != null) {
            jqf.delete(qOrderReceiptItem).where(qOrderReceiptItem.receiptId.eq(orderReceipt.getId())).execute();
            jqf.delete(qOrderReceiptCollection).where(qOrderReceiptCollection.receiptId.eq(Math.toIntExact(orderReceipt.getId()))).execute();
        }
        List<OrderReceiptItem> items = dto.getItemList();
        List<OrderReceiptCollection> collections = dto.getCollectionList();
        validateReceiptVerificationRules(orderReceipt, items);
        if (CollectionUtils.isEmpty(items)) {
            orderReceipt.setOrderType(2);
        } else {
            orderReceipt.setOrderType(1);
        }

        if (dto.getOrderReceipt().getOrderStatus() == null) {
            throw new ServiceException("状态为空");
        } else {
            if (OrderStatus.已审核.equals(dto.getOrderReceipt().getOrderStatus())) {
                if (dto.getOrderReceipt().getApprovedBy() == null) {
                    throw new ServiceException("已审核状态,审核人必填");
                }
                dto.getOrderReceipt().setApprovedAt(LocalDateTime.now());
            }
        }

        orderReceipt.setCreatedAt(LocalDateTime.now());

        // 计算销售单明细
        BigDecimal totalDocumentAmount = BigDecimal.ZERO;
        BigDecimal totalVerifiedAmount = BigDecimal.ZERO;
        BigDecimal totalCurrentVerifyAmount = BigDecimal.ZERO;

        if (items != null && !items.isEmpty()) {
            for (OrderReceiptItem item : items) {
                totalDocumentAmount = totalDocumentAmount.add(item.getDocumentAmount() == null ? BigDecimal.ZERO : item.getDocumentAmount());
                totalVerifiedAmount = totalVerifiedAmount.add(item.getVerifiedAmount() == null ? BigDecimal.ZERO : item.getVerifiedAmount());
                totalCurrentVerifyAmount = totalCurrentVerifyAmount.add(item.getCurrentVerifyAmount() == null ? BigDecimal.ZERO : item.getCurrentVerifyAmount());
            }
        }

        // 计算收款账户总金额
        BigDecimal totalPaymentAmount = BigDecimal.ZERO;
        if (!collections.isEmpty()) {
            for (OrderReceiptCollection collection : collections) {
                totalPaymentAmount = totalPaymentAmount.add(collection.getAmount() == null ? BigDecimal.ZERO : collection.getAmount());
            }
        }

        if (orderReceipt.getDiscountAmount() != null && orderReceipt.getDiscountAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("折扣金额不能为负数");
        }
        // 获取折扣金额
        BigDecimal discountAmount = orderReceipt.getDiscountAmount() == null ? BigDecimal.ZERO : orderReceipt.getDiscountAmount();

        // 应核销金额 = 订单总额 + 折扣（取自源单金额，不是实收金额）
        BigDecimal shouldVerifyAmount = totalDocumentAmount.add(discountAmount);
        orderReceipt.setCollectionAmount(totalPaymentAmount); // 收款金额
        orderReceipt.setDiscountAmount(discountAmount); // 整单折扣
        orderReceipt.setShouldVerificationAmount(shouldVerifyAmount); //应核销金额
        orderReceipt.setHasVerificationAmount(totalCurrentVerifyAmount); // 已核销金额（本单）
        orderReceipt.setVerificationAmount(totalCurrentVerifyAmount); // 本次核销金额
        // 未核销金额 = 应核销 - 本单已核销
        BigDecimal notVerifyAmount = shouldVerifyAmount.subtract(totalCurrentVerifyAmount);
        orderReceipt.setNotVerificationAmount(notVerifyAmount);
        // 预收款金额 = 本单收款 - 本次核销
        orderReceipt.setAdvanceCollectionsAmount(totalPaymentAmount.subtract(totalCurrentVerifyAmount));
        writeOffStatus(items, orderReceipt);

        updateYourBalance(orderReceipt);
        if (orderReceipt.getId() == null) {
            theOrderNumberIsAssigned(orderReceipt);
            orderReceipt.setUpdatedAt(LocalDateTime.now());
            if (orderReceipt.getOrderStatus() == null) {
                orderReceipt.setOrderStatus(OrderStatus.已保存);
            }
            orderReceipt = orderReceiptRepository.save(orderReceipt);
            saveItems(orderReceipt, items);
            saveCollections(orderReceipt, collections);
            return orderReceipt;
        } else {

            OrderReceipt original = orderReceiptRepository.getById(orderReceipt.getId());
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            BeanUtil.copyProperties(orderReceipt, original, CopyOptions.create().ignoreNullValue());
            saveItems(orderReceipt, items);
            saveCollections(orderReceipt, collections);
            if (orderReceipt.getId() != null && OrderStatus.已审核.equals(orderReceipt.getOrderStatus())) {
                calculateTheAmount(orderReceipt, OrderStatus.已审核);
            }
            return orderReceiptRepository.save(original);
        }
    }

    /**
     * 校验收款单明细是否符合核销规则
     */
    private void validateReceiptVerificationRules(OrderReceipt orderReceipt, List<OrderReceiptItem> items) {
        if (CollectionUtils.isEmpty(items)) {
            return;
        }

        Long merchantId = orderReceipt.getMerchantId();
        Long accountBookId = orderReceipt.getAccountBookId();
        Long customerId = orderReceipt.getCustomerId();

        Set<Long> salesOrderIdSet = new HashSet<>();
        boolean boo = true;
        for (OrderReceiptItem item : items) {
            BigDecimal currentVerifyAmount = item.getCurrentVerifyAmount();
            if (currentVerifyAmount == null || currentVerifyAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("核销金额不能为负数或空");
            }

            Long salesOrderId = item.getSalesOrderId();
            if (salesOrderId == null) {
                throw new ServiceException("销售单ID为空");
            }
            if (salesOrderIdSet.contains(salesOrderId)) {
                throw new ServiceException("不能重复引用销售单：" + salesOrderId);
            }
            if (salesOrderId == -1) {
                boo = false;
                Customer customer = customerService.findById(orderReceipt.getCustomerId());
                if (customer == null) {
                    throw new ServiceException("客户不存在");
                }
                BigDecimal availableAdvance = customer.getBalance() != null ? customer.getBalance() : BigDecimal.ZERO;
                if (availableAdvance.compareTo(currentVerifyAmount) < 0) {
                    throw new ServiceException("客户预收款余额不足，无法进行核销");
                }
            } else {

                salesOrderIdSet.add(salesOrderId);

                SalesOutbound salesOrder = salesOrderRepository.findById(salesOrderId)
                        .orElseThrow(() -> new ServiceException("销售单不存在：" + salesOrderId));
                if (!OrderStatus.已审核.equals(salesOrder.getOrderStatus())) {
                    throw new ServiceException("销售单未审核，无法引用：" + salesOrderId);
                }
                if (!salesOrder.getCustomerId().equals(customerId)) {
                    throw new ServiceException("销售单客户不一致，无法引用：" + salesOrderId);
                }
                BigDecimal verifiedAmount = jqf.select(qItem.currentVerifyAmount.sum())
                        .from(qItem)
                        .where(qItem.salesOrderId.eq(salesOrderId))
                        .fetchOne();

                if (verifiedAmount == null) {
                    verifiedAmount = BigDecimal.ZERO;
                }

                BigDecimal documentAmount = salesOrder.getFinalAmount();
                BigDecimal unverifiedAmount = documentAmount.subtract(verifiedAmount);
                if (unverifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("销售单已全部核销，无法再次引用：" + salesOrderId);
                }

                if (currentVerifyAmount.compareTo(unverifiedAmount) > 0) {
                    throw new ServiceException("核销金额超过销售单剩余未核销金额：" + salesOrderId);
                }

                // 检查是否有其他单据重复引用
                BigDecimal totalUsedInOtherReceipts = jqf.select(qItem.currentVerifyAmount.sum())
                        .from(qItem)
                        .leftJoin(qOrderReceipt).on(qOrderReceipt.id.eq(qItem.receiptId))
                        .where(qItem.salesOrderId.eq(salesOrderId)
                                .and(qOrderReceipt.orderStatus.eq(OrderStatus.已审核))
                                .and(qOrderReceipt.merchantId.eq(merchantId))
                                .and(qOrderReceipt.accountBookId.eq(accountBookId)))
                        .fetchOne();

                if (totalUsedInOtherReceipts == null) {
                    totalUsedInOtherReceipts = BigDecimal.ZERO;
                }

                // alreadyUsed = 其他已审核收款单对该销售单已使用的核销金额
                // verifiedAmount 已在前面按单明细校验过，不应重复累加
                BigDecimal alreadyUsed = totalUsedInOtherReceipts;
                BigDecimal maxAllowed = documentAmount;

                if (alreadyUsed.add(currentVerifyAmount).compareTo(maxAllowed) > 0) {
                    throw new ServiceException("与其他已审核单据冲突，核销金额将超出销售单总额：" + salesOrderId);
                }
            }
        }
        if (boo) {
            BigDecimal totalCurrentVerifyAmount = items.stream()
                    .map(OrderReceiptItem::getCurrentVerifyAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalDocumentAmount = salesOrderIdSet.stream()
                    .map(salesOrderId -> salesOrderRepository.findById(salesOrderId).get().getFinalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (totalCurrentVerifyAmount.compareTo(totalDocumentAmount) > 0) {
                throw new ServiceException("所有引用销售单的核销金额总和不能超过总应收金额");
            }
        }
    }

    private static void writeOffStatus(List<OrderReceiptItem> items, OrderReceipt orderReceipt) {
        if (CollectionUtils.isEmpty(items)) {
            orderReceipt.setWriteOffStatus(0); // 无明细 → 未核销
        } else if (orderReceipt.getHasVerificationAmount().compareTo(BigDecimal.ZERO) <= 0) {
            orderReceipt.setWriteOffStatus(0); // 无核销记录
        } else if (orderReceipt.getHasVerificationAmount().compareTo(orderReceipt.getShouldVerificationAmount()) >= 0) {
            orderReceipt.setWriteOffStatus(2); // 全部核销
        } else {
            orderReceipt.setWriteOffStatus(1); // 部分核销
        }
    }

    private void updateYourBalance(OrderReceipt orderReceipt) {
        if (orderReceipt.getId() != null && OrderStatus.已审核.equals(orderReceipt.getOrderStatus())) {
            if (orderReceipt.getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            orderReceipt.setApprovedAt(LocalDateTime.now());
        }
    }

    private void theOrderNumberIsAssigned(OrderReceipt orderReceipt) {
        if (StrUtil.isEmpty(orderReceipt.getOrderNo())) {
            CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(CodeRule.DocumentType.收款单, orderReceipt.getMerchantId(), orderReceipt.getAccountBookId());

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
                    JPAQuery<Long> query = jqf.select(qOrderReceipt.id.count()).from(qOrderReceipt).where(qOrderReceipt.merchantId.eq(orderReceipt.getMerchantId()).and(qOrderReceipt.accountBookId.eq(orderReceipt.getAccountBookId())));
                    Long count = query.fetchOne();
                    Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                    String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                    codeBuilder.append(serialStr);
                }
                orderReceipt.setOrderNo(codeBuilder.toString());
            } else {
                orderReceipt.setOrderNo(CodeGenerator.generateCode());
            }
        }
    }

    // 保存明细项
    private void saveItems(OrderReceipt orderReceipt, List<OrderReceiptItem> items) {
        if (items != null && !items.isEmpty()) {
            for (OrderReceiptItem item : items) {
                item.setReceiptId(orderReceipt.getId());
                item.setMerchantId(orderReceipt.getMerchantId());
                item.setAccountBookId(orderReceipt.getAccountBookId());
                orderReceiptItemRepository.save(item);
                // 审核时同步更新结算单
                if (OrderStatus.已审核.equals(orderReceipt.getOrderStatus())) {
                    settlementService.updateWriteOff(item.getSalesOrderId(), item.getCurrentVerifyAmount(),
                            orderReceipt.getMerchantId(), orderReceipt.getAccountBookId(), "销售出库单");
                }
            }
        }
    }

    // 保存结算账户信息
    private void saveCollections(OrderReceipt orderReceipt, List<OrderReceiptCollection> collections) {
        if (collections != null && !collections.isEmpty()) {
            for (OrderReceiptCollection collection : collections) {
                collection.setReceiptId(Math.toIntExact(orderReceipt.getId()));
                orderReceiptCollectionRepository.save(collection);
            }
        }
    }

    @Transactional
    public void delete(String ids, Long merchantId, Long accountBookId) {
        if (StrUtil.isBlank(ids)) {
            throw new ServiceException("请选择要删除的数据");
        }
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();

        if (idList.isEmpty()) {
            throw new ServiceException("无效的ID列表");
        }
        List<OrderReceipt> orderReceipts = jqf.select(qOrderReceipt)
                .from(qOrderReceipt)
                .where(qOrderReceipt.id.in(idList)
                        .and(qOrderReceipt.merchantId.eq(merchantId))
                        .and(qOrderReceipt.accountBookId.eq(accountBookId)))
                .fetch();

        if (orderReceipts.isEmpty()) {
            throw new ServiceException("没有找到可删除的收款单");
        }

        for (OrderReceipt receipt : orderReceipts) {
            if (!OrderStatus.已保存.equals(receipt.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的收款单：" + receipt.getOrderNo());
            }
        }

        jqf.delete(qOrderReceiptItem)
                .where(qOrderReceiptItem.receiptId.in(idList)
                        .and(qOrderReceiptItem.merchantId.eq(merchantId))
                        .and(qOrderReceiptItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(qOrderReceiptCollection)
                .where(qOrderReceiptCollection.receiptId.in(idList.stream().map(Math::toIntExact).toList()))
                .execute();
        jqf.delete(qOrderReceipt)
                .where(qOrderReceipt.id.in(idList)
                        .and(qOrderReceipt.merchantId.eq(merchantId))
                        .and(qOrderReceipt.accountBookId.eq(accountBookId)))
                .execute();
    }

    public OrderReceiptDetails load(Long merchantId, Long id) {
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser approvedNameUser = new QMerchantUser("approvedNameUser");
        OrderReceiptDetailsVO orderReceipt = jqf.select(Projections.bean(OrderReceiptDetailsVO.class,
                        qOrderReceipt.id, qOrderReceipt.customerId, qOrderReceipt.customerName, qOrderReceipt.orderType,
                        qOrderReceipt.orderDate, qOrderReceipt.orderNo, qOrderReceipt.documentSource,
                        qOrderReceipt.discountAmount, qOrderReceipt.collectionAmount, qOrderReceipt.totalAmountsOwed,
                        qOrderReceipt.verificationAmount, qOrderReceipt.advanceCollectionsAmount, qOrderReceipt.shouldVerificationAmount,
                        qOrderReceipt.hasVerificationAmount, qOrderReceipt.notVerificationAmount, qOrderReceipt.writeOffStatus,
                        qOrderReceipt.orderStatus, qOrderReceipt.orderStaffId, qOrderReceipt.orderStaffName, qOrderReceipt.createdBy,
                        qOrderReceipt.updatedBy, qOrderReceipt.createdAt, qOrderReceipt.updatedAt, qOrderReceipt.approvedBy,
                        qOrderReceipt.approvedAt, qOrderReceipt.accountBookId, qOrderReceipt.merchantId, qMerchantUser.name.as("createName"),
                        qUpdatedByUser.name.as("updateName"), approvedNameUser.name.as("approvedName")))
                .from(qOrderReceipt).leftJoin(qCustomer).on(qCustomer.id.eq(qOrderReceipt.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOrderReceipt.createdBy)).leftJoin(qUpdatedByUser)
                .on(qMerchantUser.id.eq(qOrderReceipt.updatedBy)).leftJoin(approvedNameUser).on(approvedNameUser.id.eq(qOrderReceipt.approvedBy)).where(qOrderReceipt.merchantId.eq(merchantId).and(qOrderReceipt.id.eq(id))).fetchOne();
        if (orderReceipt == null) {
            throw new ServiceException("单据不存在");
        }

        OrderReceiptDetails dto = new OrderReceiptDetails();
        dto.setOrderReceipt(orderReceipt);

        List<OrderReceiptCollection> collectionList = jqf.select(qOrderReceiptCollection).from(qOrderReceiptCollection).where(qOrderReceiptCollection.receiptId.eq(orderReceipt.getId().intValue())).fetch().stream().distinct().toList();
        dto.setCollectionList(collectionList);

        List<OrderReceiptItem> itemList = jqf.select(qOrderReceiptItem).from(qOrderReceiptItem).where(qOrderReceiptItem.receiptId.eq(orderReceipt.getId())).fetch().stream().distinct().toList();
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
    public void updateStatus(OrderPaymentUpdateDTO orderReceipt) {
        String ids = orderReceipt.getId();
        OrderStatus targetStatus = orderReceipt.getOrderStatus();
        if (targetStatus == null) {
            throw new ServiceException("请选择要操作的状态");
        }
        if (orderReceipt.getApprovedBy() == null) {
            throw new ServiceException("请选择审核人");
        }
        if (StrUtil.isBlank(ids)) {
            throw new ServiceException("请选择要操作的数据");
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();

        if (idList.isEmpty()) {
            throw new ServiceException("无效的ID列表");
        }

        List<OrderReceipt> receipts = jqf.select(qOrderReceipt)
                .from(qOrderReceipt)
                .where(qOrderReceipt.id.in(idList))
                .fetch();

        if (receipts.isEmpty()) {
            throw new ServiceException("没有找到可操作的收款单");
        }

        for (OrderReceipt receipt : receipts) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(receipt.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + receipt.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(receipt.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + receipt.getOrderNo());
            }
        }

        LocalDateTime now = LocalDateTime.now();

        jqf.update(qOrderReceipt)
                .set(qOrderReceipt.orderStatus, targetStatus)
                .set(qOrderReceipt.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qOrderReceipt.approvedBy, targetStatus == OrderStatus.已审核 ? orderReceipt.getApprovedBy() : null)
                .where(qOrderReceipt.id.in(idList))
                .execute();

        for (OrderReceipt receipt : receipts) {
            calculateTheAmount(receipt, targetStatus);
            // 审核时同步更新结算单
            if (targetStatus == OrderStatus.已审核) {
                List<OrderReceiptItem> items = jqf.select(qOrderReceiptItem)
                        .from(qOrderReceiptItem).where(qOrderReceiptItem.receiptId.eq(receipt.getId())).fetch();
                for (OrderReceiptItem item : items) {
                    settlementService.updateWriteOff(item.getSalesOrderId(), item.getCurrentVerifyAmount(),
                            receipt.getMerchantId(), receipt.getAccountBookId(), "销售出库单");
                }
            }
        }
    }

    @Transactional
    public void calculateTheAmount(OrderReceipt receipt, OrderStatus targetStatus) {
        if (targetStatus == OrderStatus.已保存 && receipt.getOrderStatus() == OrderStatus.已审核) {
            Boolean exists = jqf.select(qVerificationItem.id.isNotNull())
                    .from(qVerificationItem)
                    .leftJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId))
                    .where(
                            qVerificationItem.businessId.eq(receipt.getId().intValue())
                                    .and(qVerificationItem.businessType.eq(1))
                                    .and(qVerification.type.eq(1))
//                                    .and(qVerification.orderStatus.eq(OrderStatus.已审核))
                    )
                    .fetchFirst() != null;

            if (exists) {
                throw new ServiceException("该收款单已被核销单引用，无法进行反审核");
            }
        }
        Customer customer = customerService.findById(receipt.getCustomerId());
        BigDecimal shouldVerifyAmount = receipt.getShouldVerificationAmount();
        if (shouldVerifyAmount == null || shouldVerifyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("应核销金额必须大于零");
        }
        List<OrderReceiptCollection> collections = jqf.select(qOrderReceiptCollection).from(qOrderReceiptCollection).where(qOrderReceiptCollection.receiptId.eq(receipt.getId().intValue())).fetch().stream().distinct().toList();
        if (collections.isEmpty()) {
            throw new ServiceException("未找到结算账户信息");
        }
//        BigDecimal actualTotal = collections.stream()
//                .map(OrderReceiptCollection::getAmount)
//                .filter(Objects::nonNull)
//                .reduce(BigDecimal.ZERO, BigDecimal::add);
//
//        if (actualTotal.compareTo(shouldVerifyAmount) != 0) {

        if (targetStatus == OrderStatus.已审核) {
            customer.setBalance(customer.getBalance().subtract(shouldVerifyAmount));
        } else {
            if (receipt.getOrderStatus() != OrderStatus.已审核) {
                throw new ServiceException("只有已审核的单据才能反审核");
            }
            customer.setBalance(customer.getBalance().add(shouldVerifyAmount));
        }
        CustomerFlow customerFlow = getCustomerFlow(receipt, targetStatus, customer);
        customerService.updateTheBalance(customer, customerFlow);
        for (OrderReceiptCollection collection : collections) {
            Long accountId = collection.getSettlementAccountId();
            if (accountId == null) {
                throw new ServiceException("结算账户不能为空");
            }

            BigDecimal amount = collection.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("金额必须大于零");
            }

            AccountBalanceChangeContext context = AccountBalanceChangeContext.builder()
                    .accountId(accountId)
                    .merchantId(receipt.getMerchantId())
                    .accountBookId(receipt.getAccountBookId())
                    .voucherId(receipt.getId())
                    .customerId(receipt.getCustomerId())
                    .businessNo(receipt.getOrderNo())
                    .flowType(AccountFlow.AccountFlowType.收款单)
                    .operatorId(receipt.getOrderStaffId())
                    .correspondentsId(receipt.getCustomerId())
                    .correspondentsName(receipt.getCustomerName())
                    .operatorName(receipt.getOrderStaffName())
                    .remarks(targetStatus == OrderStatus.已审核 ? "收款单审核通过" : "收款单反审核")
                    .build();

            if (targetStatus == OrderStatus.已审核) {
                context.setAmount(amount);
            } else {
                context.setAmount(amount.negate());
            }
            accountService.updateAccountBalanceWithFlow(context);
        }

    }

    private static @NotNull CustomerFlow getCustomerFlow(OrderReceipt receipt, OrderStatus targetStatus, Customer customer) {
        CustomerFlow.CustomerFlowType flowType;
        CustomerFlow customerFlow = new CustomerFlow();
        customerFlow.setCustomerId(customer.getId());
        customerFlow.setBusinessId(receipt.getId());
        customerFlow.setBusinessNo(receipt.getOrderNo());
        customerFlow.setBusinessDate(receipt.getOrderDate());

        BigDecimal collectionAmount = receipt.getCollectionAmount();
        if (targetStatus == OrderStatus.已审核) {
            flowType = CustomerFlow.CustomerFlowType.收款单;
            customerFlow.setSalesAmount(collectionAmount);
        } else {
            flowType = CustomerFlow.CustomerFlowType.反审核_收款单;
            customerFlow.setSalesAmount(collectionAmount != null ? collectionAmount.negate() : BigDecimal.ZERO);
        }
        customerFlow.setCustomerFlowType(flowType);

        customerFlow.setBalanceReceivables(customer.getBalance());
        customerFlow.setAccountBookId(receipt.getAccountBookId());
        customerFlow.setMerchantId(receipt.getMerchantId());
        customerFlow.setCreatedBy(receipt.getApprovedBy());
        customerFlow.setCreatedAt(LocalDateTime.now());
        customerFlow.setRemarks(targetStatus == OrderStatus.已审核 ? "收款单审核通过" : "收款单反审核");

        return customerFlow;
    }

    public Object writeOffCandidates(Page page, OrderReceiptService.SalesQuery query) {
        if (query.getCustomerId() == null) {
            throw new ServiceException("客户ID不能为空");
        }

        NumberExpression<BigDecimal> receiptVerifySum = qOrderReceiptItem.currentVerifyAmount.sum()
                .coalesce(BigDecimal.ZERO);

        NumberExpression<BigDecimal> verificationVerifySum = Expressions.numberTemplate(
                BigDecimal.class,
                "COALESCE(SUM(CASE WHEN {0} IS NOT NULL THEN {1} ELSE 0 END), 0)",
                qVerification.id,
                qVerificationItem.currentVerifyAmount
        ).coalesce(BigDecimal.ZERO);

        NumberExpression<BigDecimal> totalVerifiedExpr = receiptVerifySum.add(verificationVerifySum);
        NumberExpression<BigDecimal> unverifiedExpr = qSalesOutbound.finalAmount.subtract(totalVerifiedExpr);
        JPAQuery<SalesOrderWithVerification> mainQuery = jqf.select(
                        Projections.fields(
                                SalesOrderWithVerification.class,
                                qSalesOutbound.id.as("salesOrderId"),
                                qSalesOutbound.orderNo.as("salesOrderNo"),
                                qSalesOutbound.outboundDate.as("businessDate"),
                                qSalesOutbound.finalAmount.as("documentAmount"),
                                totalVerifiedExpr.as("verifiedAmount"),
                                unverifiedExpr.as("unverifiedAmount")
                        )
                )
                .from(qSalesOutbound)
                .leftJoin(qOrderReceiptItem).on(qOrderReceiptItem.salesOrderId.eq(qSalesOutbound.id))
                .leftJoin(qVerificationItem).on(
                        qVerificationItem.businessId.eq(qSalesOutbound.id.intValue())
                                .and(qVerificationItem.businessType.eq(1)))
                .leftJoin(qVerification).on(
                        qVerification.id.eq(qVerificationItem.verificationId)
                                .and(qVerification.orderStatus.eq(OrderStatus.已审核)))
                .where(query.builder.and(qSalesOutbound.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(qSalesOutbound.id, qSalesOutbound.orderNo, qSalesOutbound.outboundDate, qSalesOutbound.finalAmount)
                .having(unverifiedExpr.gt(BigDecimal.ZERO));

        List<SalesOrderWithVerification> result = mainQuery.offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        long total = mainQuery.fetchCount();

        return new PageResults<>(result, page, total);
    }

    @JsonInclude()
    @Data
    public static class SalesOrderWithVerification {
        private Long salesOrderId;
        private String salesOrderNo;
        private Integer businessType = 1;
        private LocalDate businessDate;
        private BigDecimal documentAmount;
        private BigDecimal verifiedAmount;
        private BigDecimal unverifiedAmount;

    }

    public static class SalesQuery implements TenantAware {
        @Getter
        Long customerId;

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qSalesOutbound.merchantId, merchantId);
        }

        public void setOrderNo(String orderNo) {
            if (orderNo != null) {
                builder.and(qSalesOutbound.orderNo.like("%" + orderNo + "%"));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qSalesOutbound.accountBookId, accountBookId);
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
            }
        }
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qOrderReceipt)
                .select(qOrderReceipt.collectionAmount.sum())
                .where(query.builder).fetchFirst();
    }

    public static class Query implements TenantAware {

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qOrderReceipt.customerId.eq(customerId));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qOrderReceipt.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qOrderReceipt.accountBookId, accountBookId);
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qOrderReceipt.orderStatus.eq(orderStatus));
            }
        }

        public void setOrderType(Integer orderType) {
            if (orderType != null) {
                builder.and(qOrderReceipt.orderType.eq(orderType));
            }
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotEmpty(startTime)) {
                builder.and(qOrderReceipt.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotEmpty(endTime)) {
                builder.and(qOrderReceipt.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }

        public void setKeyword(String keyword) {
            if (keyword != null && !keyword.isEmpty()) {
                builder.and(qOrderReceipt.orderNo.like("%" + keyword + "%").or(qOrderReceipt.customerName.like("%" + keyword + "%")));
            }
        }

        public void setWriteOff(Integer writeOff) {
            if (writeOff != null && writeOff == 1) {
                builder.and(qOrderReceipt.notVerificationAmount.gt(BigDecimal.ZERO));
            }
        }

        public void setAdvance(Integer advance) {
            if (advance != null && advance == 1) {
                builder.and(qOrderReceipt.advanceCollectionsAmount.gt(BigDecimal.ZERO));
            }
        }

    }

    public static class ReceivableDetailReportQuery implements TenantAware {

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qOrderReceipt.merchantId, merchantId);
        }

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qOrderReceipt.customerId.eq(customerId));
            }
        }

        public void setWriteOff(Integer writeOff) {
            if (writeOff != null && writeOff == 1) {
                builder.and(qOrderReceipt.notVerificationAmount.gt(BigDecimal.ZERO));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qOrderReceipt.accountBookId, accountBookId);
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qOrderReceipt.orderStatus.eq(orderStatus));
            }
        }

        public void setStartTime(String startTime) {
            if (startTime != null && !startTime.isEmpty()) {
                LocalDate startDate = LocalDate.parse(startTime);
                LocalDateTime startDateTime = startDate.atStartOfDay();
                builder.and(qOrderReceipt.createdAt.goe(startDateTime));
            }
        }

        public void setEndTime(String endTime) {
            if (endTime != null && !endTime.isEmpty()) {
                LocalDate endDate = LocalDate.parse(endTime);
                LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
                builder.and(qOrderReceipt.createdAt.loe(endDateTime));
            }
        }

        public void setKeyword(String keyword) {
            if (keyword != null && !keyword.isEmpty()) {
                builder.and(qOrderReceipt.orderNo.like("%" + keyword + "%").or(qOrderReceipt.customerName.like("%" + keyword + "%")));
            }
        }
    }
}
