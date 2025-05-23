package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.QPaymentMethod;
import com.flyemu.share.entity.basic.QSupplier;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.purchase.PurchaseOrder;
import com.flyemu.share.entity.purchase.QPurchaseOrder;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.*;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.fund.dto.OrderPaymentSaveDTO;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.vo.OrderPaymentDetails;
import com.flyemu.share.service.fund.vo.OrderPaymentDetailsVO;
import com.flyemu.share.service.fund.vo.OrderPaymentQueryVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
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
 * @author q
 * @功能描述: 付款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderPaymentService extends AbsService {

    private final static QOrderPayment qOrderPayment = QOrderPayment.orderPayment;

    private final OrderPaymentRepository orderPaymentRepository;
    private final static QOrderPaymentItem qItem = QOrderPaymentItem.orderPaymentItem;
    private final QOrderPaymentCollection qCollection = QOrderPaymentCollection.orderPaymentCollection;
    private final QSupplier qSupplier = QSupplier.supplier;

    private final static QPaymentMethod qPaymentMethod = QPaymentMethod.paymentMethod;
    private final CodeRuleService codeRuleService;
    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository quantityRepository;
    private final OrderPaymentItemRepository orderPaymentItemRepository;
    private final OrderPaymentCollectionRepository orderPaymentCollectionRepository;
    private final static QMerchantUser qMerchantUser = QMerchantUser.merchantUser;

    private final static QOrderPaymentItem QorderPaymentItem = QOrderPaymentItem.orderPaymentItem;
    private final static QOrderPaymentCollection QorderPaymentCollection = QOrderPaymentCollection.orderPaymentCollection;

    public PageResults<OrderPayment> query(Page page, OrderPaymentService.Query query) {
        PagedList<OrderPayment> fetchPage = bqf.selectFrom(qOrderPayment).where(query.builder).orderBy(qOrderPayment.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OrderPayment> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OrderPayment orderPayment1 = tuple;
            OrderPayment orderPayment = BeanUtil.toBean(orderPayment1, OrderPayment.class);
            dtos.add(orderPayment);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OrderPayment save(OrderPaymentSaveDTO dto) {
        if (dto.getOrderPayment() == null) {
            throw new ServiceException("参数错误");
        }
        if (dto.getCollectionList() == null) {
            throw new ServiceException("参数错误");
        }

        OrderPayment orderPayment = dto.getOrderPayment();
        List<OrderPaymentItem> items = dto.getItemList();
        List<OrderPaymentCollection> collections = dto.getCollectionList();
        validatePaymentVerificationRules(orderPayment, items);
        if (CollectionUtils.isEmpty(items)) {
            orderPayment.setOrderType(1); // 预付款单
        } else {
            orderPayment.setOrderType(2); // 付款单
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

        // 应核销金额 = 实际支付 + 折扣
        BigDecimal shouldVerifyAmount = totalPaymentAmount.add(discountAmount);


        orderPayment.setCollectionAmount(totalPaymentAmount);
        orderPayment.setDiscountAmount(discountAmount);
        orderPayment.setShouldVerificationAmount(shouldVerifyAmount);
        orderPayment.setHasVerificationAmount(totalVerifiedAmount.add(totalCurrentVerifyAmount));
        orderPayment.setVerificationAmount(totalCurrentVerifyAmount);
        // 未核销金额 = 应核销金额 - 已核销金额
        BigDecimal notVerifyAmount = shouldVerifyAmount.subtract(orderPayment.getHasVerificationAmount());
        orderPayment.setNotVerificationAmount(notVerifyAmount);
        orderPayment.setAdvanceCollectionsAmount(notVerifyAmount); // 预付款金额 = 未核销金额


        writeOffStatus(items, orderPayment);

        // 更新供应商余额
        updateSupplierBalance(orderPayment);

        if (orderPayment.getId() == null) {
            assignOrderNumber(orderPayment);
            orderPayment.setUpdateAt(LocalDateTime.now());
            if (orderPayment.getOrderStatus() == null) {
                orderPayment.setOrderStatus(OrderStatus.已保存);
            }
            orderPayment = orderPaymentRepository.save(orderPayment);
            saveItems(orderPayment, items);
            saveCollections(orderPayment, collections);
            return orderPayment;
        } else {
            OrderPayment original = orderPaymentRepository.getById(orderPayment.getId());
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            BeanUtil.copyProperties(orderPayment, original, CopyOptions.create().ignoreNullValue());
            jqf.delete(QorderPaymentItem).where(QorderPaymentItem.paymentId.eq(orderPayment.getId())).execute();
            jqf.delete(QorderPaymentCollection).where(QorderPaymentCollection.paymentId.eq(Math.toIntExact(orderPayment.getId()))).execute();
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
        if (StringUtils.isEmpty(orderPayment.getOrderNo())) {
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
                    JPAQuery<Long> query = jqf.select(qOrderPayment.id.count()).from(qOrderPayment)
                            .where(qOrderPayment.merchantId.eq(orderPayment.getMerchantId())
                                    .and(qOrderPayment.accountBookId.eq(orderPayment.getAccountBookId())));
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

    private void updateSupplierBalance(OrderPayment orderPayment) {
        if (orderPayment.getId() != null && OrderStatus.已审核.equals(orderPayment.getOrderStatus())) {
            if (orderPayment.getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            orderPayment.setApprovedAt(LocalDateTime.now());
            Supplier supplier = supplierRepository.findById(orderPayment.getSupplierId())
                    .orElseThrow(() -> new ServiceException("供应商不存在"));
            supplier.setBalance(supplier.getBalance().subtract(orderPayment.getShouldVerificationAmount()));
            supplierRepository.save(supplier);
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
        List<OrderPayment> orderPayments = jqf.select(qOrderPayment)
                .from(qOrderPayment)
                .where(qOrderPayment.id.in(idList)
                        .and(qOrderPayment.merchantId.eq(merchantId))
                        .and(qOrderPayment.accountBookId.eq(accountBookId)))
                .fetch();

        if (orderPayments.isEmpty()) {
            throw new ServiceException("没有找到可删除的付款单");
        }
        for (OrderPayment payment : orderPayments) {
            if (!OrderStatus.已保存.equals(payment.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的付款单：" + payment.getOrderNo());
            }
        }
        jqf.delete(QorderPaymentItem)
                .where(QorderPaymentItem.paymentId.in(idList)
                        .and(QorderPaymentItem.merchantId.eq(merchantId))
                        .and(QorderPaymentItem.accountBookId.eq(accountBookId)))
                .execute();
        jqf.delete(QorderPaymentCollection)
                .where(QorderPaymentCollection.paymentId.in(idList.stream().map(Math::toIntExact).toList()))
                .execute();

        jqf.delete(qOrderPayment)
                .where(qOrderPayment.id.in(idList)
                        .and(qOrderPayment.merchantId.eq(merchantId))
                        .and(qOrderPayment.accountBookId.eq(accountBookId)))
                .execute();
    }


    public PageResults<OrderPaymentQueryVO> query(OrderPaymentService.Query query, Page page) {
        JPAQuery<OrderPayment> mainQuery = jqf.select(qOrderPayment).from(qOrderPayment).where(query.builder);

        List<OrderPayment> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        long total = mainQuery.fetchCount();

        List<OrderPaymentQueryVO> voList = new ArrayList<>();

        for (OrderPayment payment : mainList) {
            OrderPaymentQueryVO vo = BeanUtil.toBean(payment, OrderPaymentQueryVO.class);
            List<OrderPaymentCollection> paymentMethods = jqf.select(Projections.bean(
                            OrderPaymentCollection.class,
                            qCollection.id,
                            qCollection.paymentId,
                            qCollection.settlementAccount,
                            qCollection.paymentMethodId,
                            qCollection.amount,
                            qCollection.remarks,
                            qCollection.paymentMethodName,
                            qCollection.theOnlineTransactionNumber))
                    .from(qCollection)
                    .where(qCollection.paymentId.eq(Math.toIntExact(payment.getId())))
                    .fetch().stream().distinct().toList();

            vo.setCollectionList(paymentMethods);
            List<OrderPaymentItem> items = jqf.select(qItem)
                    .from(qItem)
                    .where(qItem.paymentId.eq(payment.getId()))
                    .fetch().stream().distinct().toList();
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

        Set<Long> purchaseOrderIdSet = new HashSet<>();

        for (OrderPaymentItem item : items) {
            Long purchaseOrderId = item.getBusinessId();
            if (purchaseOrderIdSet.contains(purchaseOrderId)) {
                throw new ServiceException("不能重复引用采购单：" + purchaseOrderId);
            }
            purchaseOrderIdSet.add(purchaseOrderId);

            PurchaseOrder purchaseOrder = quantityRepository.findById(purchaseOrderId)
                    .orElseThrow(() -> new ServiceException("采购单不存在：" + purchaseOrderId));

            if (!OrderStatus.已审核.equals(purchaseOrder.getOrderStatus())) {
                throw new ServiceException("采购单未审核，无法引用：" + purchaseOrderId);
            }

            if (!purchaseOrder.getSupplierId().equals(supplierId)) {
                throw new ServiceException("采购单供应商不一致，无法引用：" + purchaseOrderId);
            }

            BigDecimal verifiedAmount = jqf.select(qItem.currentVerifyAmount.sum())
                    .from(qItem)
                    .where(qItem.businessId.eq(purchaseOrderId))
                    .fetchOne();

            if (verifiedAmount == null) {
                verifiedAmount = BigDecimal.ZERO;
            }

            BigDecimal documentAmount = purchaseOrder.getFinalAmount();
            BigDecimal unverifiedAmount = documentAmount.subtract(verifiedAmount);
            if (unverifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("采购单已全部核销，无法再次引用：" + purchaseOrderId);
            }

            // 当前核销金额不能大于剩余未核销金额
            BigDecimal currentVerifyAmount = item.getCurrentVerifyAmount();
            if (currentVerifyAmount == null || currentVerifyAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("核销金额不能为负数或空：" + purchaseOrderId);
            }

            if (currentVerifyAmount.compareTo(unverifiedAmount) > 0) {
                throw new ServiceException("核销金额超过采购单剩余未核销金额：" + purchaseOrderId);
            }

            // 检查是否有单据重复引用
            BigDecimal totalUsedInOtherPayments = jqf.select(qItem.currentVerifyAmount.sum())
                    .from(qItem)
                    .leftJoin(qOrderPayment).on(qOrderPayment.id.eq(qItem.paymentId))
                    .where(qItem.businessId.eq(purchaseOrderId)
                            .and(qOrderPayment.orderStatus.eq(OrderStatus.已审核))
                            .and(qOrderPayment.merchantId.eq(merchantId))
                            .and(qOrderPayment.accountBookId.eq(accountBookId)))
                    .fetchOne();

            if (totalUsedInOtherPayments == null) {
                totalUsedInOtherPayments = BigDecimal.ZERO;
            }

            BigDecimal alreadyUsed = totalUsedInOtherPayments.add(verifiedAmount); // 已被使用的总金额
            BigDecimal maxAllowed = documentAmount; // 总应付金额

            if (alreadyUsed.add(currentVerifyAmount).compareTo(maxAllowed) > 0) {
                throw new ServiceException("与其他已审核单据冲突，核销金额将超出采购单总额：" + purchaseOrderId);
            }
        }

        BigDecimal totalCurrentVerifyAmount = items.stream()
                .map(OrderPaymentItem::getCurrentVerifyAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDocumentAmount = purchaseOrderIdSet.stream()
                .map(purchaseOrderId -> quantityRepository.findById(purchaseOrderId).get().getFinalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalCurrentVerifyAmount.compareTo(totalDocumentAmount) > 0) {
            throw new ServiceException("所有引用采购单的核销金额总和不能超过总应付金额");
        }
    }

    public OrderPaymentDetails selectById(Long id) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        OrderPaymentDetailsVO orderPayment = jqf.select(Projections.bean(
                        OrderPaymentDetailsVO.class,
                        qOrderPayment.id,
                        qOrderPayment.supplierId,
                        qOrderPayment.supplierName,
                        qOrderPayment.orderType,
                        qOrderPayment.orderDate,
                        qOrderPayment.orderNo,
                        qOrderPayment.documentSource,
                        qOrderPayment.discountAmount,
                        qOrderPayment.collectionAmount,
                        qOrderPayment.totalAmountsOwed,
                        qOrderPayment.verificationAmount,
                        qOrderPayment.advanceCollectionsAmount,
                        qOrderPayment.shouldVerificationAmount,
                        qOrderPayment.hasVerificationAmount,
                        qOrderPayment.notVerificationAmount,
                        qOrderPayment.writeOffStatus,
                        qOrderPayment.orderStatus,
                        qOrderPayment.orderStaffId,
                        qOrderPayment.orderStaffName,
                        qOrderPayment.createdBy,
                        qOrderPayment.updateBy,
                        qOrderPayment.createdAt,
                        qOrderPayment.updateAt,
                        qOrderPayment.approvedBy,
                        qOrderPayment.approvedAt,
                        qOrderPayment.accountBookId,
                        qOrderPayment.merchantId,
                        qCreatedByUser.name.as("creatorName"),
                        qUpdatedByUser.name.as("updateName"),
                        qApprovedByUser.name.as("approvedName")))
                .from(qOrderPayment)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOrderPayment.createdBy))
                .leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qOrderPayment.updateBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOrderPayment.approvedBy))
                .where(qOrderPayment.id.eq(id))
                .fetchOne();

        if (orderPayment == null) {
            throw new ServiceException("付款单不存在");
        }

        OrderPaymentDetails dto = new OrderPaymentDetails();
        dto.setOrderReceipt(orderPayment);

        List<OrderPaymentCollection> collectionList = jqf.select(qCollection)
                .from(qCollection)
                .where(qCollection.paymentId.eq(Math.toIntExact(orderPayment.getId())))
                .fetch()
                .stream()
                .distinct()
                .toList();

        dto.setCollectionList(collectionList);

        List<OrderPaymentItem> itemList = jqf.select(qItem)
                .from(qItem)
                .where(qItem.paymentId.eq(orderPayment.getId()))
                .fetch()
                .stream()
                .distinct()
                .toList();

        dto.setItemList(itemList);

        return dto;
    }


    @Transactional
    public void updateStatus(OrderPaymentUpdateDTO orderPayment) {
        String ids = orderPayment.getId();
        OrderStatus targetStatus = orderPayment.getOrderStatus();
        if (targetStatus == null){
            throw new ServiceException("请选择要操作的状态");
        }
        if (orderPayment.getApprovedBy()==null){
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
        List<OrderPayment> payments = jqf.select(qOrderPayment)
                .from(qOrderPayment)
                .where(qOrderPayment.id.in(idList))
                .fetch();

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
        jqf.update(qOrderPayment)
                .set(qOrderPayment.orderStatus, targetStatus)
                .set(qOrderPayment.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qOrderPayment.approvedBy, targetStatus == OrderStatus.已审核 ? orderPayment.getApprovedBy() : null)
                .where(qOrderPayment.id.in(idList))
                .execute();

        for (OrderPayment payment : payments) {
            Supplier supplier = supplierRepository.findById(payment.getSupplierId())
                    .orElseThrow(() -> new ServiceException("供应商不存在"));
            BigDecimal verifyAmount = payment.getVerificationAmount();
            if (verifyAmount == null) {
                verifyAmount = BigDecimal.ZERO;
            }
            if (targetStatus == OrderStatus.已审核) {
                supplier.setBalance(supplier.getBalance().subtract(verifyAmount));
            } else {
                supplier.setBalance(supplier.getBalance().add(verifyAmount));
            }
            supplierRepository.save(supplier);
        }
    }

    public Object aListSalesOrders(Page page, SupplerQuery query) {
        QPurchaseOrder qPurchaseOrder = QPurchaseOrder.purchaseOrder;
        JPAQuery<PurchaseOrderWithVerification> mainQuery = jqf.select(Projections.fields(
                        PurchaseOrderWithVerification.class,
                        qPurchaseOrder.id.as("salesOrderId"),
                        qPurchaseOrder.orderNo.as("salesOrderNo"),
                        qPurchaseOrder.orderDate.as("businessDate"),
                        qPurchaseOrder.finalAmount.as("documentAmount"),
                        qItem.currentVerifyAmount.sum().as("verifiedAmount"),
                        qPurchaseOrder.finalAmount.subtract(qItem.currentVerifyAmount.sum()).as("unverifiedAmount"))
                )
                .from(qPurchaseOrder)
                .leftJoin(qItem).on(qItem.businessId.eq(qPurchaseOrder.id))
                .where(query.builder.and(qPurchaseOrder.orderStatus.eq(OrderStatus.已审核)))
                .groupBy(qPurchaseOrder.id);

        mainQuery.having(qPurchaseOrder.finalAmount
                .subtract(qItem.currentVerifyAmount.sum())
                .gt(BigDecimal.ZERO));

        List<PurchaseOrderWithVerification> result = mainQuery.offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

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

    private final static QPurchaseOrder qSuppler = QPurchaseOrder.purchaseOrder;

    public class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOrderPayment.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOrderPayment.accountBookId.eq(accountBookId));
            }
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qOrderPayment.orderStatus.eq(orderStatus));
            }
        }

        public void setStartTime(LocalDateTime startTime) {
            if (startTime != null) {
                builder.and(qOrderPayment.createdAt.goe(startTime));
            }
        }

        public void setEndTime(LocalDateTime endTime) {
            if (endTime != null) {
                builder.and(qOrderPayment.createdAt.loe(endTime));
            }
        }

        public void setKeyword(String keyword) {
            if (StringUtils.isNotBlank(keyword)) {
                builder.and(qOrderPayment.orderNo.like("%" + keyword + "%")
                        .or(qSupplier.name.like("%" + keyword + "%")));
            }
        }

    }

    public static class SupplerQuery {
        @Getter
        Long supplierId;

        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSuppler.merchantId.eq(merchantId));
            }
        }

        public void setOrderNo(String orderNo) {
            if (orderNo != null) {
                builder.and(qSuppler.orderNo.like("%" + orderNo + "%"));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSuppler.accountBookId.eq(accountBookId));
            }
        }

        public void setCustomerId(Long supplierId) {
            this.supplierId = supplierId;
            if (supplierId != null) {
                builder.and(qSuppler.supplierId.eq(supplierId));
            }

        }
    }
}
