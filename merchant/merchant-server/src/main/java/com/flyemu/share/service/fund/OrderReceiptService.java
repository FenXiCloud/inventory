package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QPaymentMethod;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.QSalesOutbound;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.*;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.dto.OrderReceiptSaveDTO;
import com.flyemu.share.service.fund.vo.*;
import com.flyemu.share.service.fund.vo.report.ReceivableDetailReportVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @author shuaiqi
 * @功能描述: 收款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderReceiptService extends AbsService {

    private final static QOrderReceipt qOrderReceipt = QOrderReceipt.orderReceipt;
    private final static QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
    private final static QVerification qVerification = QVerification.verification;
    private final static QOrderReceiptItem qItem = QOrderReceiptItem.orderReceiptItem;
    private final QOrderReceiptCollection qCollection = QOrderReceiptCollection.orderReceiptCollection;
    private final QCustomer qCustomer = QCustomer.customer;

    private final static QPaymentMethod qPaymentMethod = QPaymentMethod.paymentMethod;
    private final OrderReceiptRepository orderReceiptRepository;
    private final CodeRuleService codeRuleService;
    private final CustomerService customerService;
    private final AccountService accountService;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderReceiptItemRepository orderReceiptItemRepository;
    private final OrderReceiptCollectionRepository orderReceiptCollectionRepository;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final static QOrderReceiptItem qOrderReceiptItem = QOrderReceiptItem.orderReceiptItem;
    private final static QOrderReceiptCollection qOrderReceiptCollection = QOrderReceiptCollection.orderReceiptCollection;

    public PageResults<ReceivableDetailReportVO> getReceivableDetailReport(Page page, ReceivableDetailReportQuery query) {
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
                                qReceipt.shouldVerificationAmount.subtract(qReceipt.hasVerificationAmount).as("balance"),
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
        JPAQuery<OrderReceipt> mainQuery = jqf.select(qOrderReceipt).from(qOrderReceipt).where(query.builder).orderBy(qOrderReceipt.orderDate.desc());

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
    public OrderReceipt save(OrderReceiptSaveDTO dto) {
        if (dto.getOrderReceipt() == null) {
            throw new ServiceException("主订单参数错误");
        }
        if (dto.getCollectionList() == null) {
            throw new ServiceException("账户参数错误");
        }
        OrderReceipt orderReceipt = dto.getOrderReceipt();
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

        // 计算应核销金额 = 收款金额 + 折扣
        BigDecimal shouldVerifyAmount = totalPaymentAmount.add(discountAmount);
        orderReceipt.setCollectionAmount(totalPaymentAmount); // 收款金额
        orderReceipt.setDiscountAmount(discountAmount); // 整单折扣
        orderReceipt.setShouldVerificationAmount(shouldVerifyAmount); //应核销金额
        orderReceipt.setHasVerificationAmount(totalVerifiedAmount.add(totalCurrentVerifyAmount)); // 已核销金额
        orderReceipt.setVerificationAmount(totalCurrentVerifyAmount); // 本次核销金额
        // 未核销金额 = 应核销金额 - 已核销金额
        BigDecimal notVerifyAmount = shouldVerifyAmount.subtract(orderReceipt.getHasVerificationAmount());
        orderReceipt.setNotVerificationAmount(notVerifyAmount);
        orderReceipt.setAdvanceCollectionsAmount(notVerifyAmount); // 预收款金额 = 未核销金额
        writeOffStatus(items, orderReceipt);


        updateYourBalance(orderReceipt);
        if (orderReceipt.getId() == null) {
            theOrderNumberIsAssigned(orderReceipt);
            orderReceipt.setUpdateAt(LocalDateTime.now());
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

        for (OrderReceiptItem item : items) {
            Long salesOrderId = item.getSalesOrderId();
            if (salesOrderId == null) {
                throw new ServiceException("销售单ID为空");
            }
            if (salesOrderIdSet.contains(salesOrderId)) {
                throw new ServiceException("不能重复引用销售单：" + salesOrderId);
            }
            salesOrderIdSet.add(salesOrderId);

            SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId)
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

            // 当前核销金额不能大于剩余未核销金额
            BigDecimal currentVerifyAmount = item.getCurrentVerifyAmount();
            if (currentVerifyAmount == null || currentVerifyAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("核销金额不能为负数或空：" + salesOrderId);
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

            BigDecimal alreadyUsed = totalUsedInOtherReceipts.add(verifiedAmount); // 已被使用的总金额
            BigDecimal maxAllowed = documentAmount; // 总应收金额

            if (alreadyUsed.add(currentVerifyAmount).compareTo(maxAllowed) > 0) {
                throw new ServiceException("与其他已审核单据冲突，核销金额将超出销售单总额：" + salesOrderId);
            }
        }
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
        if (StringUtils.isEmpty(orderReceipt.getOrderNo())) {
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
        if (StringUtils.isBlank(ids)) {
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

    public OrderReceiptDetails selectById(Long id) {
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser approvedNameUser = new QMerchantUser("approvedNameUser");
        OrderReceiptDetailsVO orderReceipt = jqf.select(Projections.bean(OrderReceiptDetailsVO.class,
                        qOrderReceipt.id, qOrderReceipt.customerId, qOrderReceipt.customerName, qOrderReceipt.orderType,
                        qOrderReceipt.orderDate, qOrderReceipt.orderNo, qOrderReceipt.documentSource,
                        qOrderReceipt.discountAmount, qOrderReceipt.collectionAmount, qOrderReceipt.totalAmountsOwed,
                        qOrderReceipt.verificationAmount, qOrderReceipt.advanceCollectionsAmount, qOrderReceipt.shouldVerificationAmount,
                        qOrderReceipt.hasVerificationAmount, qOrderReceipt.notVerificationAmount, qOrderReceipt.writeOffStatus,
                        qOrderReceipt.orderStatus, qOrderReceipt.orderStaffId, qOrderReceipt.orderStaffName, qOrderReceipt.createdBy,
                        qOrderReceipt.updateBy, qOrderReceipt.createdAt, qOrderReceipt.updateAt, qOrderReceipt.approvedBy,
                        qOrderReceipt.approvedAt, qOrderReceipt.accountBookId, qOrderReceipt.merchantId, qMerchantUser.name.as("createName"),
                        qUpdatedByUser.name.as("updateName"), approvedNameUser.name.as("approvedName")))
                .from(qOrderReceipt).leftJoin(qCustomer).on(qCustomer.id.eq(qOrderReceipt.customerId))
                .leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOrderReceipt.createdBy)).leftJoin(qUpdatedByUser)
                .on(qMerchantUser.id.eq(qOrderReceipt.updateBy)).leftJoin(approvedNameUser).on(approvedNameUser.id.eq(qOrderReceipt.approvedBy)).where(qOrderReceipt.id.eq(id)).fetchOne();
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
    public void updateStatus(OrderPaymentUpdateDTO orderReceipt) {
        String ids = orderReceipt.getId();
        OrderStatus targetStatus = orderReceipt.getOrderStatus();
        if (targetStatus == null) {
            throw new ServiceException("请选择要操作的状态");
        }
        if (orderReceipt.getApprovedBy() == null) {
            throw new ServiceException("请选择审核人");
        }
        if (StringUtils.isBlank(ids)) {
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
//            throw new ServiceException("实际到账金额与应核销金额不一致，无法审核");
//        }

        if (targetStatus == OrderStatus.已审核) {
            customer.setBalance(customer.getBalance().subtract(shouldVerifyAmount));
        } else {
            if (receipt.getOrderStatus() != OrderStatus.已审核) {
                throw new ServiceException("只有已审核的单据才能反审核");
            }
            customer.setBalance(customer.getBalance().add(shouldVerifyAmount));
        }
        customerService.updateTheBalance(customer);
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


    private final static QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;


    public Object aListSalesOrders(Page page, OrderReceiptService.SalesQuery query) {
        if (query.getCustomerId() == null) {
            throw new ServiceException("客户ID不能为空");
        }
        Customer customer = customerService.findById(query.getCustomerId());
        QSalesOutbound qSalesOutbound = QSalesOutbound.salesOutbound;

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

    public static class SalesQuery {
        @Getter
        Long customerId;

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOutbound.merchantId.eq(merchantId));
            }
        }

        public void setOrderNo(String orderNo) {
            if (orderNo != null) {
                builder.and(qSalesOutbound.orderNo.like("%" + orderNo + "%"));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOutbound.accountBookId.eq(accountBookId));
            }
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
            if (customerId != null) {
                builder.and(qSalesOutbound.customerId.eq(customerId));
            }
        }
    }

    public static class Query {

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setCustomerId(Long customerId) {
            if (customerId != null) {
                builder.and(qOrderReceipt.customerId.eq(customerId));
            }
        }

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOrderReceipt.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOrderReceipt.accountBookId.eq(accountBookId));
            }
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
            if (StringUtils.isNotEmpty(startTime)) {
                builder.and(qOrderReceipt.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StringUtils.isNotEmpty(endTime)) {
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

    }

    public static class ReceivableDetailReportQuery {

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOrderReceipt.merchantId.eq(merchantId));
            }
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
            if (accountBookId != null) {
                builder.and(qOrderReceipt.accountBookId.eq(accountBookId));
            }
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qOrderReceipt.orderStatus.eq(orderStatus));
            }
        }

        public void setStartTime(LocalDateTime startTime) {
            if (startTime != null) {
                builder.and(qOrderReceipt.createdAt.goe(startTime));
            }
        }

        public void setEndTime(LocalDateTime endTime) {
            if (endTime != null) {
                builder.and(qOrderReceipt.createdAt.loe(endTime));
            }
        }

        public void setKeyword(String keyword) {
            if (keyword != null && !keyword.isEmpty()) {
                builder.and(qOrderReceipt.orderNo.like("%" + keyword + "%").or(qOrderReceipt.customerName.like("%" + keyword + "%")));
            }
        }
    }
}
