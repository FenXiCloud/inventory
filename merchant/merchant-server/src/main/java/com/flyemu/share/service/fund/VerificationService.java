package com.flyemu.share.service.fund;

import com.flyemu.share.repository.basic.CustomerRepository;
import com.flyemu.share.repository.basic.SupplierRepository;
import com.flyemu.share.repository.fund.OrderPaymentRepository;
import com.flyemu.share.repository.fund.OrderReceiptRepository;
import com.flyemu.share.repository.fund.VerificationCollectionRepository;
import com.flyemu.share.repository.fund.VerificationItemRepository;
import com.flyemu.share.repository.fund.VerificationRepository;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.form.VerificationForm;
import com.flyemu.share.service.fund.vo.VerificationDetails;
import com.flyemu.share.service.fund.vo.VerificationQueryVO;
import com.flyemu.share.service.fund.vo.VerificationVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class VerificationService extends BaseService {

    private final CheckoutService checkoutService;
    private final static QVerification qVerification = QVerification.verification;
    private final static QVerificationItem qVerificationItem = QVerificationItem.verificationItem;
    private final static QVerificationCollection qVerificationCollection = QVerificationCollection.verificationCollection;

    private final VerificationRepository verificationRepository;
    private final VerificationItemRepository verificationItemRepository;
    private final VerificationCollectionRepository verificationCollectionRepository;
    private final CustomerRepository customerRepository;
    private final SupplierRepository supplierRepository;
    private final OrderReceiptRepository orderReceiptRepository;
    private final OrderPaymentRepository orderPaymentRepository;
    private final CodeRuleService codeRuleService;
    private final SettlementService settlementService;

    @Transactional
    public Verification save(VerificationForm dto) {
        java.time.LocalDate __checkoutOrderDate = dto.getOrder() == null || dto.getOrder().getOrderDate() == null ? null : dto.getOrder().getOrderDate().toLocalDate();
        if (dto.getOrder() != null) {
            checkoutService.assertEditable(dto.getOrder().getMerchantId(), dto.getOrder().getAccountBookId(), __checkoutOrderDate);
        }
        if (dto.getOrder() == null) {
            throw new ServiceException("核销单主表信息不能为空");
        }
        if (dto.getOrder().getOrderStatus() == null) {
            throw new ServiceException("状态为空");
        }
        if (dto.getOrder().getType() == null) {
            throw new ServiceException("类型为空");
        }
        if (OrderStatus.已审核.equals(dto.getOrder().getOrderStatus())) {
            if (dto.getOrder().getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            dto.getOrder().setApprovedAt(LocalDateTime.now());
        }
        List<VerificationItem> items = dto.getItemList();
        List<VerificationCollection> collections = dto.getCollectionList();

        if (items == null || items.isEmpty()) {
            throw new ServiceException("核销明细不能为空");
        }
        if (collections == null || collections.isEmpty()) {
            throw new ServiceException("核销单据不能为空");
        }
        validateVerificationItems(dto.getOrder(), dto.getCollectionList());
        Verification verification = dto.getOrder();

        if (verification.getId() != null) {
            Verification original = verificationRepository.findById(verification.getId())
                    .orElseThrow(() -> new ServiceException("核销单不存在"));

            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }

            jqf.delete(qVerificationItem)
                    .where(qVerificationItem.verificationId.eq(verification.getId()))
                    .execute();
            jqf.delete(qVerificationCollection)
                    .where(qVerificationCollection.verificationId.eq(verification.getId()))
                    .execute();
        }

        if (verification.getId() == null) {
            verification.setCreatedAt(LocalDateTime.now());
            assignOrderNumber(verification);
        } else {
            verification.setUpdatedAt(LocalDateTime.now());
            Verification byId = verificationRepository.getById(verification.getId());
            if (!OrderStatus.已保存.equals(byId.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
        }

        verification = verificationRepository.save(verification);

        subtableProcessing(items, verification, collections);

        if (OrderStatus.已审核.equals(verification.getOrderStatus())) {
            int direction = 1;
            // 处理预收款单
            handleOrderReceiptOrPayment(verification, direction);
            // 同步更新结算单
            if (items != null) {
                for (VerificationItem item : items) {
                    if (item.getBusinessId() != null && item.getCurrentVerifyAmount() != null) {
                        settlementService.updateWriteOff(
                                item.getBusinessId().longValue(),
                                item.getCurrentVerifyAmount(),
                                verification.getMerchantId(),
                                verification.getAccountBookId());
                    }
                }
            }
        }

        return verification;
    }

    private void subtableProcessing(List<VerificationItem> items, Verification verification, List<VerificationCollection> collections) {
        // 保存明细项
        if (!items.isEmpty()) {
            for (VerificationItem item : items) {
                item.setVerificationId(verification.getId());
                item.setMerchantId(verification.getMerchantId());
                item.setAccountBookId(verification.getAccountBookId());
                verificationItemRepository.save(item);
            }
        }

        // 保存收款账户明细
        if (collections != null && !collections.isEmpty()) {
            BigDecimal totalCollectionAmount = BigDecimal.ZERO;

            for (VerificationCollection collection : collections) {
                collection.setVerificationId(verification.getId());
                collection.setMerchantId(verification.getMerchantId());
                collection.setAccountBookId(verification.getAccountBookId());

                if (collection.getCurrentVerifyAmount() != null) {
                    totalCollectionAmount = totalCollectionAmount.add(collection.getCurrentVerifyAmount());
                }

                verificationCollectionRepository.save(collection);
            }

            //收款账户总金额必须等于明细本次核销金额总和
            BigDecimal totalItemVerifyAmount = items.stream()
                    .map(VerificationItem::getCurrentVerifyAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalCollectionAmount.compareTo(totalItemVerifyAmount) != 0) {
                throw new ServiceException("核销单总金额必须等于所选订单的本次核销金额总和");
            }
        }
    }

    /**
     * 核销单明细校验
     */
    private void validateVerificationItems(Verification verification, List<VerificationCollection> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        Set<Integer> businessIdSet = new HashSet<>();

        for (VerificationCollection item : items) {
            Integer businessId = item.getBusinessId();
            Integer businessType = verification.getType();
            if (item.getUnverifiedAmount() == null){
                throw new ServiceException("未核销金额不能为空");
            }
            if(item.getUnverifiedAmount().compareTo(BigDecimal.ZERO) < 0){
                throw new ServiceException("未核销金额不能小于0");
            }
            if (businessIdSet.contains(businessId)) {
                throw new ServiceException("不能重复引用同一订单：" + businessId);
            }
            businessIdSet.add(businessId);

            BigDecimal totalAmount;
            BigDecimal verifiedAmount;

            // 判断是否是虚拟订单
            if (businessId == -1) {
                Long personnelId = verification.getPersonnelId();
                Customer customer = customerRepository.findById(personnelId)
                        .orElseThrow(() -> new ServiceException("客户不存在"));

                totalAmount = customer.getBalance();
                verifiedAmount = BigDecimal.ZERO;
            } else {
                totalAmount = getOrderTotalAmount(businessType, businessId);
                verifiedAmount = getTotalVerifiedAmount(businessType, businessId);
            }

            // 剩余未核销金额
            BigDecimal unverifiedAmount = totalAmount.subtract(verifiedAmount);
            BigDecimal currentVerifyAmount = item.getCurrentVerifyAmount();

            if (currentVerifyAmount == null || currentVerifyAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new ServiceException("核销金额必须大于0：" + businessId);
            }

            if (currentVerifyAmount.compareTo(unverifiedAmount) > 0) {
                throw new ServiceException("核销金额超过订单剩余未核销金额：" + businessId);
            }
        }
    }

    private BigDecimal getTotalVerifiedAmount(Integer businessType, Integer businessId) {
        if (businessId == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal verifiedFromOriginal = BigDecimal.ZERO;
        BigDecimal verifiedFromVerifications = BigDecimal.ZERO;

        if (businessType == 1) {
            // 查询收款单明细中对该订单的核销总额
            QOrderReceiptItem qItem = QOrderReceiptItem.orderReceiptItem;
            QOrderReceipt qR = QOrderReceipt.orderReceipt;
            BigDecimal sum = jqf.select(qItem.currentVerifyAmount.sum())
                    .from(qItem)
                    .join(qR).on(qR.id.eq(qItem.receiptId))
                    .where(qItem.salesOrderId.eq(businessId.longValue())
                            .and(qR.orderStatus.eq(OrderStatus.已审核)))
                    .fetchOne();
            verifiedFromOriginal = sum != null ? sum : BigDecimal.ZERO;
        } else if (businessType == 2) {
            // 查询付款单明细中对该订单的核销总额
            QOrderPaymentItem qPi = QOrderPaymentItem.orderPaymentItem;
            QOrderPayment qP = QOrderPayment.orderPayment;
            BigDecimal sum = jqf.select(qPi.currentVerifyAmount.sum())
                    .from(qPi)
                    .join(qP).on(qP.id.eq(qPi.paymentId))
                    .where(qPi.businessId.eq(businessId.longValue())
                            .and(qP.orderStatus.eq(OrderStatus.已审核)))
                    .fetchOne();
            verifiedFromOriginal = sum != null ? sum : BigDecimal.ZERO;
        }
        verifiedFromVerifications = jqf.select(qVerificationItem.currentVerifyAmount.sum())
                .from(qVerificationItem)
                .leftJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId))
                .where(
                        qVerificationItem.businessId.eq(businessId)
                                .and(qVerificationItem.businessType.eq(businessType))
                                .and(qVerification.orderStatus.eq(OrderStatus.已审核))
                )
                .fetchOne();

        return Optional.of(verifiedFromOriginal).orElse(BigDecimal.ZERO)
                .add(Optional.ofNullable(verifiedFromVerifications).orElse(BigDecimal.ZERO));

    }

    private BigDecimal getOrderTotalAmount(Integer businessType, Integer businessId) {
        if (businessType == 1) {
            OrderReceipt orderReceipt = orderReceiptRepository.findById(businessId.longValue())
                    .orElseThrow(() -> new ServiceException("收款单不存在：" + businessId));
            return orderReceipt.getShouldVerificationAmount();
        } else if (businessType == 2) {
            OrderPayment orderPayment = orderPaymentRepository.findById(businessId.longValue())
                    .orElseThrow(() -> new ServiceException("付款单不存在：" + businessId));
            return orderPayment.getShouldVerificationAmount();
        } else {
            throw new ServiceException("不支持的业务类型");
        }
    }

    private void assignOrderNumber(Verification verification) {
        if (StrUtil.isNotBlank(verification.getOrderNo())) {
            return;
        }

        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.核销单,
                verification.getMerchantId(),
                verification.getAccountBookId());

        StringBuilder codeBuilder = new StringBuilder();

        if (codeRule != null) {
            if (StrUtil.isNotBlank(codeRule.getPrefix())) {
                codeBuilder.append(codeRule.getPrefix());
            }

            if (StrUtil.isNotBlank(codeRule.getFormat())) {
                String formattedDate = DateUtil.format(new Date(), codeRule.getFormat());
                codeBuilder.append(formattedDate);
            }

            Integer serialLength = codeRule.getSerialNumberLength();
            if (serialLength != null && serialLength > 0) {
                Long count = jqf.select(qVerification.id.count())
                        .from(qVerification)
                        .where(qVerification.merchantId.eq(verification.getMerchantId())
                                .and(qVerification.accountBookId.eq(verification.getAccountBookId())))
                        .fetchOne();

                Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                codeBuilder.append(serialStr);
            }
        } else {
            codeBuilder.append(CodeGenerator.generateCode());
        }

        verification.setOrderNo(codeBuilder.toString());
    }

    /**
     * 更新客户或供应商余额
     */
    private void updateBalance(Verification verification, List<VerificationItem> items, int direction) {
        BigDecimal totalVerifyAmount = BigDecimal.ZERO;
        for (VerificationItem item : items) {
            if (item.getCurrentVerifyAmount() != null) {
                totalVerifyAmount = totalVerifyAmount.add(item.getCurrentVerifyAmount());
            }
        }

        if (totalVerifyAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (verification.getType() == 1) {
            Long customerId = verification.getPersonnelId();
            Customer customer = customerRepository.findById(customerId)
                    .orElseThrow(() -> new ServiceException("客户不存在"));

            if (direction == 1) {
                customer.setBalance(customer.getBalance().subtract(totalVerifyAmount));
            } else {
                customer.setBalance(customer.getBalance().add(totalVerifyAmount));
            }
            customerRepository.save(customer);
        } else if (verification.getType() == 2) {
            Long supplierId = verification.getPersonnelId();
            Supplier supplier = supplierRepository.findById(supplierId)
                    .orElseThrow(() -> new ServiceException("供应商不存在"));

            if (direction == 1) {
                supplier.setBalance(supplier.getBalance().subtract(totalVerifyAmount));
            } else {
                supplier.setBalance(supplier.getBalance().add(totalVerifyAmount));
            }
            supplierRepository.save(supplier);
        }
    }

    public PageResults<VerificationQueryVO> query(Page page, VerificationService.Query query) {
        JPAQuery<Verification> mainQuery = jqf.select(qVerification).from(qVerification).where(query.builder).orderBy(qVerification.id.desc());
        List<Verification> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        long total = mainQuery.fetchCount();
        List<VerificationQueryVO> voList = new ArrayList<>();
        for (Verification verification : mainList) {
            VerificationQueryVO vo = BeanUtil.toBean(verification, VerificationQueryVO.class);
            List<VerificationCollection> collections = jqf.select(
                            qVerificationCollection)
                    .from(qVerificationCollection)
                    .where(qVerificationCollection.verificationId.eq(verification.getId()))
                    .fetch();
            vo.setCollectionList(collections);
            List<VerificationItem> items = jqf.select(qVerificationItem)
                    .from(qVerificationItem)
                    .where(qVerificationItem.verificationId.eq(verification.getId()))
                    .fetch();
            vo.setItemList(items);
            voList.add(vo);
        }
        return new PageResults<>(voList, page, total);
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
    public void updateStatus(OrderPaymentUpdateDTO dto) {
        String ids = dto.getId();
        OrderStatus targetStatus = dto.getOrderStatus();

        if (StrUtil.isBlank(ids)) {
            throw new ServiceException("请选择要操作的数据");
        }
        if (targetStatus == null) {
            throw new ServiceException("请选择要操作的状态");
        }
        if (dto.getApprovedBy() == null) {
            throw new ServiceException("请选择审核人");
        }

        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::valueOf)
                .toList();

        if (idList.isEmpty()) {
            throw new ServiceException("无效的ID列表");
        }

        List<Verification> verifications = jqf.select(qVerification)
                .from(qVerification)
                .where(qVerification.id.in(idList))
                .fetch();

        if (verifications.isEmpty()) {
            throw new ServiceException("没有找到可操作的核销单");
        }

        for (Verification verification : verifications) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(verification.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + verification.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(verification.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + verification.getOrderNo());
            }
        }

        LocalDateTime now = LocalDateTime.now();

        jqf.update(qVerification)
                .set(qVerification.orderStatus, targetStatus)
                .set(qVerification.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qVerification.approvedBy, targetStatus == OrderStatus.已审核 ? dto.getApprovedBy().intValue() : null)
                .where(qVerification.id.in(idList))
                .execute();

        boolean isAudit = OrderStatus.已审核.equals(targetStatus);
        int direction = isAudit ? 1 : -1;

        for (Verification verification : verifications) {
            List<VerificationItem> items = jqf.select(qVerificationItem)
                    .from(qVerificationItem)
                    .where(qVerificationItem.verificationId.eq(verification.getId()))
                    .fetch();

            if (items == null || items.isEmpty()) {
                throw new ServiceException("核销明细不能为空");
            }
            handleOrderReceiptOrPayment(verification, direction);
            // 审核时同步更新结算单
            if (isAudit) {
                for (VerificationItem item : items) {
                    if (item.getBusinessId() != null && item.getCurrentVerifyAmount() != null) {
                        settlementService.updateWriteOff(
                                item.getBusinessId().longValue(),
                                item.getCurrentVerifyAmount(),
                                verification.getMerchantId(),
                                verification.getAccountBookId());
                    }
                }
            }
        }
    }

    private void handleOrderReceiptOrPayment(Verification verification, int direction) {
        List<VerificationCollection> collections = jqf.select(qVerificationCollection)
                .from(qVerificationCollection)
                .where(qVerificationCollection.verificationId.eq(verification.getId()))
                .fetch();
        for (VerificationCollection collection : collections) {
            Integer businessType = verification.getType();
            Integer businessId = collection.getBusinessId();
            BigDecimal verifyAmount = collection.getCurrentVerifyAmount();

            if (verifyAmount == null || verifyAmount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (businessId == -1) {
                continue;
            }
            boolean isVerify = direction == 1;
            if (businessType == 1) {
                OrderReceipt receipt = orderReceiptRepository.findById(Long.valueOf(businessId))
                        .orElseThrow(() -> new ServiceException("收款单不存在"));
                updateReceiptVerificationStatus(receipt, verifyAmount, isVerify);
                orderReceiptRepository.save(receipt);

            } else if (businessType == 2) {
                OrderPayment payment = orderPaymentRepository.findById(Long.valueOf(businessId))
                        .orElseThrow(() -> new ServiceException("付款单不存在"));
                updatePaymentVerificationStatus(payment, verifyAmount, isVerify);
                orderPaymentRepository.save(payment);
            }
        }
    }

    private void updateReceiptVerificationStatus(OrderReceipt receipt, BigDecimal verifyAmount, boolean isVerify) {
        if (receipt.getHasVerificationAmount() == null) {
            receipt.setHasVerificationAmount(BigDecimal.ZERO);
        }
        BigDecimal newVerifiedAmount = isVerify ?
                receipt.getHasVerificationAmount().add(verifyAmount) :
                receipt.getHasVerificationAmount().subtract(verifyAmount);

        if (newVerifiedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("收款单【" + receipt.getOrderNo() + "】本单已核销金额不能为负数");
        }

        if (newVerifiedAmount.compareTo(receipt.getCollectionAmount() != null ? receipt.getCollectionAmount() : receipt.getShouldVerificationAmount()) > 0) {
            throw new ServiceException("收款单【" + receipt.getOrderNo() + "】本单已核销金额不能超过收款金额");
        }

        receipt.setHasVerificationAmount(newVerifiedAmount);
        receipt.setNotVerificationAmount(receipt.getShouldVerificationAmount().subtract(newVerifiedAmount));
        // 同步扣减预收款余额
        receipt.setAdvanceCollectionsAmount(
                (receipt.getCollectionAmount() != null ? receipt.getCollectionAmount() : BigDecimal.ZERO)
                        .subtract(newVerifiedAmount));
        if (newVerifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            receipt.setWriteOffStatus(0); // 未核销
        } else if (newVerifiedAmount.compareTo(receipt.getShouldVerificationAmount()) >= 0) {
            receipt.setWriteOffStatus(2); // 全部核销
        } else {
            receipt.setWriteOffStatus(1); // 部分核销
        }
    }

    private void updatePaymentVerificationStatus(OrderPayment payment, BigDecimal verifyAmount, boolean isVerify) {
        if (payment.getHasVerificationAmount() == null) {
            payment.setHasVerificationAmount(BigDecimal.ZERO);
        }

        BigDecimal newVerifiedAmount = isVerify ?
                payment.getHasVerificationAmount().add(verifyAmount) :
                payment.getHasVerificationAmount().subtract(verifyAmount);

        if (newVerifiedAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("付款单【" + payment.getOrderNo() + "】本单已核销金额不能为负数");
        }

        if (newVerifiedAmount.compareTo(payment.getCollectionAmount() != null ? payment.getCollectionAmount() : payment.getShouldVerificationAmount()) > 0) {
            throw new ServiceException("付款单【" + payment.getOrderNo() + "】本单已核销金额不能超过付款金额");
        }
        payment.setHasVerificationAmount(newVerifiedAmount);
        payment.setNotVerificationAmount(payment.getShouldVerificationAmount().subtract(newVerifiedAmount));
        // 同步扣减预付款余额
        payment.setAdvanceCollectionsAmount(
                (payment.getCollectionAmount() != null ? payment.getCollectionAmount() : BigDecimal.ZERO)
                        .subtract(newVerifiedAmount));

        if (newVerifiedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            payment.setWriteOffStatus(0); // 未核销
        } else if (newVerifiedAmount.compareTo(payment.getShouldVerificationAmount()) >= 0) {
            payment.setWriteOffStatus(2); // 全部核销
        } else {
            payment.setWriteOffStatus(1); // 部分核销
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

        List<Verification> verifications = jqf.select(qVerification)
                .from(qVerification)
                .where(qVerification.id.in(idList)
                        .and(qVerification.merchantId.eq(merchantId))
                        .and(qVerification.accountBookId.eq(accountBookId)))
                .fetch();

        if (verifications.isEmpty()) {
            throw new ServiceException("没有找到可删除的核销单");
        }

        for (Verification verification : verifications) {
            if (!OrderStatus.已保存.equals(verification.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的核销单：" + verification.getOrderNo());
            }
        }

        jqf.delete(qVerificationItem)
                .where(qVerificationItem.verificationId.in(idList))
                .execute();

        jqf.delete(qVerificationCollection)
                .where(qVerificationCollection.verificationId.in(idList))
                .execute();

        jqf.delete(qVerification)
                .where(qVerification.id.in(idList)
                        .and(qVerification.merchantId.eq(merchantId))
                        .and(qVerification.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<Verification> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qVerification).where(qVerification.merchantId.eq(merchantId).and(qVerification.accountBookId.eq(accountBookId))).fetch();
    }

    public VerificationDetails load(Long merchantId, Long id) {
        if (id == null) {
            throw new ServiceException("ID不能为空");
        }

        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        VerificationVO orderVO = jqf.select(
                        Projections.bean(
                                VerificationVO.class,
                                qVerification.id,
                                qVerification.orderNo,
                                qVerification.orderStatus,
                                qVerification.createdAt,
                                qVerification.approvedAt,
                                qVerification.approvedBy,
                                qVerification.type,
                                qVerification.personnelId,
                                qVerification.personnelName,
                                qVerification.createdBy,
                                qVerification.updatedBy,
                                qVerification.remarks,
                                qVerification.orderDate,
                                qVerification.orderStaffId,
                                qVerification.orderStaffName,
                                qCreatedByUser.name.as("createName"),
                                qUpdatedByUser.name.as("updateName"),
                                qApprovedByUser.name.as("approvedName"),
                                qVerification.merchantId,
                                qVerification.accountBookId
                        )
                )
                .from(qVerification)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qVerification.createdBy.longValue()))
                .leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qVerification.updatedBy.longValue()))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qVerification.approvedBy.longValue()))
                .where(qVerification.merchantId.eq(merchantId).and(qVerification.id.eq(id)))
                .fetchOne();

        if (orderVO == null) {
            throw new ServiceException("核销单不存在");
        }

        VerificationDetails details = new VerificationDetails();
        details.setOrder(orderVO);

        List<VerificationItem> items = jqf.select(qVerificationItem)
                .from(qVerificationItem)
                .where(qVerificationItem.verificationId.eq(id))
                .fetch();
        details.setItemList(items);

        List<VerificationCollection> collections = jqf.select(
                        qVerificationCollection
                )
                .from(qVerificationCollection)
                .where(qVerificationCollection.verificationId.eq(id))
                .fetch();

        details.setCollectionList(collections);

        return details;
    }

    public BigDecimal queryTotal(Query query) {
        // sum item verify amounts for matching verifications
        return bqf.selectFrom(qVerificationItem)
                .select(qVerificationItem.currentVerifyAmount.sum())
                .innerJoin(qVerification).on(qVerification.id.eq(qVerificationItem.verificationId))
                .where(query.builder).fetchFirst();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qVerification.merchantId, merchantId);
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotEmpty(startTime)) {
                builder.and(qVerification.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotEmpty(endTime)) {
                builder.and(qVerification.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }

        public void setOrderStatus(OrderStatus orderStatus) {
            if (orderStatus != null) {
                builder.and(qVerification.orderStatus.eq(orderStatus));
            }
        }

        public void setOrderType(Integer orderType) {
            if (orderType != null) {
                builder.and(qVerification.type.eq(orderType));
            }
        }

        public void setOrderNo(String orderNo) {
            if (StrUtil.isNotEmpty(orderNo)) {
                builder.and(qVerification.orderNo.like("%" + orderNo + "%"));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qVerification.accountBookId, accountBookId);
        }

    }
}
