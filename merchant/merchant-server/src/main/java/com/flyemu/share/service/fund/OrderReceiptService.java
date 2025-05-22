package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.basic.QPaymentMethod;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.sales.QSalesOrder;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.*;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.fund.dto.OrderReceiptSaveDTO;
import com.flyemu.share.service.fund.vo.*;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
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
    private final static QOrderReceiptItem qItem = QOrderReceiptItem.orderReceiptItem;
    private final QOrderReceiptCollection qCollection = QOrderReceiptCollection.orderReceiptCollection;
    private final QCustomer qCustomer = QCustomer.customer;

    private final static QPaymentMethod qPaymentMethod = QPaymentMethod.paymentMethod;
    private final OrderReceiptRepository orderReceiptRepository;
    private final CodeRuleService codeRuleService;
    private final SalesOrderRepository salesOrderRepository;
    private final OrderReceiptItemRepository orderReceiptItemRepository;
    private final OrderReceiptCollectionRepository orderReceiptCollectionRepository;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final static QOrderReceiptItem qOrderReceiptItem = QOrderReceiptItem.orderReceiptItem;
    private final static QOrderReceiptCollection qOrderReceiptCollection = QOrderReceiptCollection.orderReceiptCollection;

    public PageResults<OrderReceiptQueryVO> query(OrderReceiptService.Query query, Page page) {
        JPAQuery<OrderReceipt> mainQuery = jqf.select(qOrderReceipt).from(qOrderReceipt).where(query.builder);

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
            throw new ServiceException("参数错误");
        }
        if (dto.getCollectionList() == null) {
            throw new ServiceException("参数错误");
        }

        OrderReceipt orderReceipt = dto.getOrderReceipt();
        List<OrderReceiptItem> items = dto.getItemList();
        List<OrderReceiptCollection> collections = dto.getCollectionList();

        if (CollectionUtils.isEmpty(items)) {
            orderReceipt.setOrderType(1);
        } else {
            orderReceipt.setOrderType(2);
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
                totalPaymentAmount = totalPaymentAmount.add(collection.getAmount() == null ? BigDecimal.ZERO : BigDecimal.valueOf(collection.getAmount()));
            }
        }


        // 获取折扣金额
        BigDecimal discountAmount = orderReceipt.getDiscountAmount() == null ? BigDecimal.ZERO : orderReceipt.getDiscountAmount();

        // 计算应核销金额 = 收款金额 - 折扣
        BigDecimal shouldVerifyAmount = totalPaymentAmount.subtract(discountAmount);
        orderReceipt.setCollectionAmount(totalPaymentAmount); // 收款金额
        orderReceipt.setDiscountAmount(discountAmount); // 整单折扣
        orderReceipt.setShouldVerificationAmount(shouldVerifyAmount); //  已考虑折扣
        orderReceipt.setHasVerificationAmount(totalVerifiedAmount.add(totalCurrentVerifyAmount)); // 已核销金额
        orderReceipt.setVerificationAmount(totalCurrentVerifyAmount); // 本次核销金额
        // 未核销金额 = 应核销金额 - 已核销金额
        BigDecimal notVerifyAmount = shouldVerifyAmount.subtract(orderReceipt.getHasVerificationAmount());
        orderReceipt.setNotVerificationAmount(notVerifyAmount);
        orderReceipt.setAdvanceCollectionsAmount(notVerifyAmount); // 预收款金额 = 未核销金额
        if (CollectionUtils.isEmpty(items)) {
            orderReceipt.setWriteOffStatus(0); // 无明细 → 未核销
        } else if (orderReceipt.getHasVerificationAmount().compareTo(BigDecimal.ZERO) <= 0) {
            orderReceipt.setWriteOffStatus(0); // 无核销记录
        } else if (orderReceipt.getHasVerificationAmount().compareTo(orderReceipt.getShouldVerificationAmount()) >= 0) {
            orderReceipt.setWriteOffStatus(2); // 全部核销
        } else {
            orderReceipt.setWriteOffStatus(1); // 部分核销
        }

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
            BeanUtil.copyProperties(orderReceipt, original, CopyOptions.create().ignoreNullValue());
            jqf.delete(qOrderReceiptItem).where(qOrderReceiptItem.receiptId.eq(orderReceipt.getId())).execute();
            jqf.delete(qOrderReceiptCollection).where(qOrderReceiptCollection.receiptId.eq(Math.toIntExact(orderReceipt.getId()))).execute();
            saveItems(orderReceipt, items);
            saveCollections(orderReceipt, collections);
            return orderReceiptRepository.save(original);
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

    public VerificationInfo getVerificationInfo(Long salesOrderId) {
        // 获取销售单总金额
        SalesOrder salesOrder = salesOrderRepository.findById(salesOrderId).orElseThrow(() -> new ServiceException("销售单不存在"));

        BigDecimal documentAmount = salesOrder.getFinalAmount();

        // 查询历史已核销金额
        BigDecimal verifiedAmount = jqf.select(qItem.currentVerifyAmount.sum()).from(qItem).where(qItem.salesOrderId.eq(salesOrderId)).fetchOne();

        // 计算未核销金额
        BigDecimal unverifiedAmount = documentAmount.subtract(verifiedAmount);

        return new VerificationInfo(documentAmount, verifiedAmount, unverifiedAmount);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        OrderReceipt orderReceipt = jqf.select(qOrderReceipt).from(qOrderReceipt).where(qOrderReceipt.id.eq(supplierFlowId).and(qOrderReceipt.merchantId.eq(merchantId)).and(qOrderReceipt.accountBookId.eq(accountBookId))).fetchOne();

        if (orderReceipt == null) {
            throw new ServiceException("单据不存在");
        }
        if (OrderStatus.已审核.equals(orderReceipt.getOrderStatus())) {
            throw new ServiceException("已审核的单据无法删除");
        }
        jqf.delete(qOrderReceipt).where(qOrderReceipt.id.eq(supplierFlowId).and(qOrderReceipt.merchantId.eq(merchantId)).and(qOrderReceipt.accountBookId.eq(accountBookId))).execute();
    }

    public OrderReceiptDetails selectById(Long id) {
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser approvedNameUser = new QMerchantUser("approvedNameUser");
        OrderReceiptDetailsVO orderReceipt = jqf.select(Projections.bean(OrderReceiptDetailsVO.class, qOrderReceipt.id, qOrderReceipt.customerId, qOrderReceipt.customerName, qOrderReceipt.orderType, qOrderReceipt.orderDate, qOrderReceipt.orderNo, qOrderReceipt.documentSource, qOrderReceipt.discountAmount, qOrderReceipt.collectionAmount, qOrderReceipt.totalAmountsOwed, qOrderReceipt.verificationAmount, qOrderReceipt.advanceCollectionsAmount, qOrderReceipt.shouldVerificationAmount, qOrderReceipt.hasVerificationAmount, qOrderReceipt.notVerificationAmount, qOrderReceipt.writeOffStatus, qOrderReceipt.orderStatus, qOrderReceipt.orderStaffId, qOrderReceipt.orderStatusName, qOrderReceipt.createdBy, qOrderReceipt.updateBy, qOrderReceipt.createdAt, qOrderReceipt.updateAt, qOrderReceipt.approvedBy, qOrderReceipt.approvedAt, qOrderReceipt.accountBookId, qOrderReceipt.merchantId, qMerchantUser.name.as("creatorName"), qUpdatedByUser.name.as("updateName"), approvedNameUser.name.as("approvedName"))).from(qOrderReceipt).leftJoin(qCustomer).on(qCustomer.id.eq(qOrderReceipt.customerId)).leftJoin(qMerchantUser).on(qMerchantUser.id.eq(qOrderReceipt.createdBy)).leftJoin(qUpdatedByUser).on(qMerchantUser.id.eq(qOrderReceipt.updateBy)).leftJoin(approvedNameUser).on(approvedNameUser.id.eq(qOrderReceipt.approvedBy)).where(qOrderReceipt.id.eq(id)).fetchOne();
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
    public void updateStatus(OrderReceipt orderReceipt) {
        if (orderReceipt.getOrderStatus() == null) {
            throw new ServiceException("状态为空");
        }
        if (orderReceipt.getApprovedBy() == null) {
            throw new ServiceException("已审核状态,审核人必填");
        }
        orderReceipt.setUpdateBy(orderReceipt.getUpdateBy());
        orderReceipt.setApprovedAt(LocalDateTime.now());
        OrderReceipt existing = jqf.select(qOrderReceipt).from(qOrderReceipt).where(qOrderReceipt.id.eq(orderReceipt.getId())).fetchOne();

        if (existing == null) {
            throw new ServiceException("单据不存在");
        }

        if (!OrderStatus.已保存.equals(existing.getOrderStatus())) {
            throw new ServiceException("该单据不是已保存状态，无法审核");
        }
        orderReceipt.setUpdateAt(LocalDateTime.now());
        jqf.update(qOrderReceipt).set(qOrderReceipt.orderStatus, orderReceipt.getOrderStatus()).set(qOrderReceipt.approvedAt, LocalDateTime.now()).set(qOrderReceipt.approvedBy, orderReceipt.getApprovedBy()).where(qOrderReceipt.id.eq(orderReceipt.getId())).execute();

    }

    private final static QSalesOrder qSalesOrder = QSalesOrder.salesOrder;

    public Object aListSalesOrders(Page page, OrderReceiptService.SalesQuery query) {
        if (query.getCustomerId() == null) {
            throw new ServiceException("客户ID不能为空");
        }
        JPAQuery<SalesOrder> jpaQuery = jqf.select(qSalesOrder).from(qSalesOrder);
        jpaQuery.where(query.builder);
        jpaQuery.where(qSalesOrder.orderStatus.eq(OrderStatus.已审核));
        List<SalesOrder> salesOrders = jpaQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();

        List<SalesOrderWithVerification> result = new ArrayList<>();
        for (SalesOrder salesOrder : salesOrders) {
            BigDecimal documentAmount = salesOrder.getFinalAmount();
            BigDecimal verifiedAmount = jqf.select(qItem.currentVerifyAmount.sum()).from(qItem).where(qItem.salesOrderId.eq(salesOrder.getId())).fetchOne();
            if (verifiedAmount == null) {
                verifiedAmount = BigDecimal.ZERO;
            }
            BigDecimal unverifiedAmount = documentAmount.subtract(verifiedAmount);
            SalesOrderWithVerification vo = new SalesOrderWithVerification();
            vo.setSalesOrderId(salesOrder.getId());
            vo.setSalesOrderNo(salesOrder.getOrderNo());
            vo.setBusinessDate(salesOrder.getOrderDate());
            vo.setDocumentAmount(documentAmount);
            vo.setVerifiedAmount(verifiedAmount);
            vo.setUnverifiedAmount(unverifiedAmount);
            result.add(vo);
        }

        return result;
    }

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
        Long customerId;

        public Long getCustomerId() {
            return customerId;
        }

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSalesOrder.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSalesOrder.accountBookId.eq(accountBookId));
            }
        }

        public void setCustomerId(Long customerId) {
            this.customerId = customerId;
            if (customerId != null) {
                builder.and(qSalesOrder.customerId.eq(customerId));
            }
        }
    }

    public static class Query {

        public final BooleanBuilder builder = new BooleanBuilder();

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
