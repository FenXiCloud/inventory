package com.flyemu.share.service.fund;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.OtherReceiptItemRepository;
import com.flyemu.share.repository.OtherReceiptRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.form.OtherReceiptForm;
import com.flyemu.share.service.fund.vo.OtherReceiptDetails;
import com.flyemu.share.service.fund.vo.OtherReceiptDetailsVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @功能描述: 其他收入单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class OtherReceiptService extends AbsService {

    private final CheckoutService checkoutService;
    private final static QOtherReceipt qOtherReceipt = QOtherReceipt.otherReceipt;
    private final static QOtherReceiptItem qOtherReceiptItem = QOtherReceiptItem.otherReceiptItem;
    private final OtherReceiptItemRepository otherReceiptItemRepository;
    private final CustomerService customerService;
    private final OtherReceiptRepository otherReceiptRepository;
    private final CodeRuleService codeRuleService;
    private final AccountService accountService;

    public PageResults<OtherReceiptDetailsVO> query(Page page, OtherReceiptService.Query query) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");
        QCustomer qCustomer = QCustomer.customer;

        JPAQuery<OtherReceiptDetailsVO> mainQuery = jqf.select(Projections.bean(OtherReceiptDetailsVO.class,
                        qOtherReceipt.id,
                        qOtherReceipt.orderNo,
                        qOtherReceipt.orderDate,
                        qOtherReceipt.collectionAmount,
                        qOtherReceipt.settlementAccount,
                        qOtherReceipt.settlementAccountId,
                        qOtherReceipt.customerId,
                        qOtherReceipt.customerName,
                        qOtherReceipt.orderStaffId,
                        qOtherReceipt.orderStaffName,
                        qOtherReceipt.expirationDate,
                        qOtherReceipt.arrearsAmount,
                        qOtherReceipt.orderStatus,
                        qOtherReceipt.approvedAt,
                        qOtherReceipt.approvedBy,
                        qOtherReceipt.createdBy,
                        qOtherReceipt.createdAt,
                        qOtherReceipt.updateAt,
                        qOtherReceipt.accountBookId,
                        qOtherReceipt.merchantId,
                        qOtherReceipt.remarks,
                        qCreatedByUser.name.as("createName"),
                        qApprovedByUser.name.as("approvedName")
                ))
                .from(qOtherReceipt)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOtherReceipt.customerId))
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOtherReceipt.createdBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOtherReceipt.approvedBy))
                .where(query.builder).orderBy(qOtherReceipt.id.desc());

        List<OtherReceiptDetailsVO> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        long total = mainQuery.fetchCount();

        return new PageResults<>(mainList, page, total);
    }

    @Transactional
    public OtherReceipt save(OtherReceiptForm dto) {
        OtherReceipt otherReceipt = dto.getOrder();
        if (otherReceipt != null) {
            checkoutService.assertEditable(otherReceipt.getMerchantId(), otherReceipt.getAccountBookId(), otherReceipt.getOrderDate());
        }
        List<OtherReceiptItem> items = dto.getItemList();
        if (otherReceipt == null) {
            throw new ServiceException("参数错误");
        }
        if (otherReceipt.getCollectionAmount() == null || otherReceipt.getCollectionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("收款金额必须大于0");
        }

        if (items != null && !items.isEmpty()) {
            for (OtherReceiptItem item : items) {
                if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("明细项金额必须大于0");
                }
            }
        }
        if (otherReceipt.getCustomerId() == null) {
            throw new ServiceException("请选择客户");
        }
        if (OrderStatus.已审核.equals(otherReceipt.getOrderStatus())) {
            if (otherReceipt.getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            otherReceipt.setApprovedAt(LocalDateTime.now());
        }
        if (otherReceipt.getId() == null) {
            otherReceipt.setCreatedAt(LocalDateTime.now());
            if (otherReceipt.getOrderStatus() == null) {
                otherReceipt.setOrderStatus(OrderStatus.已保存);
            }
            assignOrderNumber(otherReceipt);
        } else {
            otherReceipt.setUpdateAt(LocalDateTime.now());
            OtherReceipt original = otherReceiptRepository.findById(otherReceipt.getId())
                    .orElseThrow(() -> new ServiceException("其他收入单不存在"));
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            jqf.delete(qOtherReceiptItem)
                    .where(qOtherReceiptItem.otherReceiptId.eq(otherReceipt.getId()))
                    .execute();
        }

        otherReceipt = otherReceiptRepository.save(otherReceipt);
        if (items != null && !items.isEmpty()) {
            for (OtherReceiptItem item : items) {
                item.setOtherReceiptId(otherReceipt.getId());
                item.setMerchantId(otherReceipt.getMerchantId());
                item.setAccountBookId(otherReceipt.getAccountBookId());
                otherReceiptItemRepository.save(item);
            }
        }
        if (OrderStatus.已审核.equals(otherReceipt.getOrderStatus())) {
            updateCustomerBalance(otherReceipt);
        }
        return otherReceipt;
    }

    private void assignOrderNumber(OtherReceipt receipt) {
        if (StringUtils.isNotBlank(receipt.getOrderNo())) {
            return;
        }

        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.其他收款单,
                receipt.getMerchantId(),
                receipt.getAccountBookId());

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
                Long count = jqf.select(qOtherReceipt.id.count())
                        .from(qOtherReceipt)
                        .where(qOtherReceipt.merchantId.eq(receipt.getMerchantId())
                                .and(qOtherReceipt.accountBookId.eq(receipt.getAccountBookId())))
                        .fetchOne();

                Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                codeBuilder.append(serialStr);
            }
        } else {
            codeBuilder.append(CodeGenerator.generateCode());
        }

        receipt.setOrderNo(codeBuilder.toString());
    }

    @Transactional
    private void updateCustomerBalance(OtherReceipt otherReceipt) {
        if (otherReceipt.getApprovedBy() == null) {
            throw new ServiceException("已审核状态,审核人必填");
        }
        otherReceipt.setApprovedAt(LocalDateTime.now());
        updateCustomerAndAccountBalances(otherReceipt, OrderStatus.已审核);
    }

    /**
     * 批量删除其他收入单（仅允许删除【已保存】状态的单据）
     */
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

        List<OtherReceipt> receiptList = jqf.selectFrom(qOtherReceipt)
                .where(qOtherReceipt.id.in(idList)
                        .and(qOtherReceipt.merchantId.eq(merchantId))
                        .and(qOtherReceipt.accountBookId.eq(accountBookId)))
                .fetch();

        if (receiptList.isEmpty()) {
            throw new ServiceException("没有找到可删除的其他收入单");
        }

        for (OtherReceipt receipt : receiptList) {
            if (!OrderStatus.已保存.equals(receipt.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的单据：" + receipt.getOrderNo());
            }
        }

        jqf.delete(qOtherReceiptItem)
                .where(qOtherReceiptItem.otherReceiptId.in(idList))
                .execute();

        jqf.delete(qOtherReceipt)
                .where(qOtherReceipt.id.in(idList)
                        .and(qOtherReceipt.merchantId.eq(merchantId))
                        .and(qOtherReceipt.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherReceipt> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherReceipt).where(qOtherReceipt.merchantId.eq(merchantId).and(qOtherReceipt.accountBookId.eq(accountBookId))).fetch();
    }

    public OtherReceiptDetails load(Long merchantId, Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("ID不能为空");
        }

        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        OtherReceiptDetailsVO otherReceiptVO = jqf.select(Projections.bean(OtherReceiptDetailsVO.class,
                        qOtherReceipt.id,
                        qOtherReceipt.customerId,
                        qOtherReceipt.customerName,
                        qOtherReceipt.settlementAccount,
                        qOtherReceipt.settlementAccountId,
                        qOtherReceipt.remarks,
                        qOtherReceipt.orderStaffId,
                        qOtherReceipt.orderStaffName,
                        qOtherReceipt.orderDate,
                        qOtherReceipt.orderNo,
                        qOtherReceipt.collectionAmount,
                        qOtherReceipt.arrearsAmount,
                        qOtherReceipt.expirationDate,
                        qOtherReceipt.orderStatus,
                        qOtherReceipt.approvedAt,
                        qOtherReceipt.approvedBy,
                        qOtherReceipt.createdBy,
                        qOtherReceipt.createdAt,
                        qOtherReceipt.updateBy,
                        qOtherReceipt.updateAt,
                        qOtherReceipt.accountBookId,
                        qOtherReceipt.merchantId,
                        qCreatedByUser.name.as("createName"),
                        qUpdatedByUser.name.as("updateName"),
                        qApprovedByUser.name.as("approvedName")
                ))
                .from(qOtherReceipt)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOtherReceipt.createdBy))
                .leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qOtherReceipt.updateBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOtherReceipt.approvedBy))
                .where(qOtherReceipt.merchantId.eq(merchantId).and(qOtherReceipt.id.eq(id)))
                .fetchOne();

        if (otherReceiptVO == null) {
            throw new ServiceException("单据不存在");
        }

        List<OtherReceiptItem> items = jqf.select(qOtherReceiptItem)
                .from(qOtherReceiptItem)
                .where(qOtherReceiptItem.otherReceiptId.eq(id))
                .fetch();
        OtherReceiptDetails details = new OtherReceiptDetails();
        details.setOrder(otherReceiptVO);
        details.setItemList(items);

        return details;
    }

    /**
     * 更新单据状态（审核/反审核）
     */
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

        if (targetStatus == null) {
            throw new ServiceException("请选择要操作的状态");
        }
        if (dto.getApprovedBy() == null) {
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
        List<OtherReceipt> receiptList = jqf.selectFrom(qOtherReceipt)
                .where(qOtherReceipt.id.in(idList))
                .fetch();

        if (receiptList.isEmpty()) {
            throw new ServiceException("没有找到可操作的其他收入单");
        }

        LocalDateTime now = LocalDateTime.now();

        for (OtherReceipt receipt : receiptList) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(receipt.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + receipt.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(receipt.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + receipt.getOrderNo());
            }
            updateCustomerAndAccountBalances(receipt, targetStatus);
            receipt.setOrderStatus(targetStatus);
            if (targetStatus == OrderStatus.已审核) {
                receipt.setApprovedAt(now);
                receipt.setApprovedBy(dto.getApprovedBy());
            } else {
                receipt.setApprovedAt(null);
                receipt.setApprovedBy(null);
            }
            otherReceiptRepository.save(receipt);

        }

        jqf.update(qOtherReceipt)
                .set(qOtherReceipt.orderStatus, targetStatus)
                .set(qOtherReceipt.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qOtherReceipt.approvedBy, targetStatus == OrderStatus.已审核 ? dto.getApprovedBy() : null)
                .where(qOtherReceipt.id.in(idList))
                .execute();
    }

    @Transactional
    public void updateCustomerAndAccountBalances(OtherReceipt receipt, OrderStatus targetStatus) {
        Customer customer = customerService.findById(receipt.getCustomerId());

        BigDecimal amount = receipt.getCollectionAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }
        if (targetStatus == OrderStatus.已审核) {
            customer.setBalance(customer.getBalance().subtract(amount));
        } else {
            if (receipt.getOrderStatus() != OrderStatus.已审核) {
                throw new ServiceException("只有已审核的付款单才能反审核");
            }
            customer.setBalance(customer.getBalance().add(amount));
        }
        CustomerFlow customerFlow = getCustomerFlow(receipt, targetStatus, customer);
        customerService.updateTheBalance(customer, customerFlow);
        if (receipt.getSettlementAccountId() == null) {
            throw new ServiceException("结算账户不能为空");
        }

        AccountBalanceChangeContext context = AccountBalanceChangeContext.builder()
                .accountId(receipt.getSettlementAccountId())
                .merchantId(receipt.getMerchantId())
                .accountBookId(receipt.getAccountBookId())
                .voucherId(receipt.getId())
                .customerId(receipt.getCustomerId())
                .businessNo(receipt.getOrderNo())
                .flowType(AccountFlow.AccountFlowType.其他收入单)
                .operatorId(receipt.getOrderStaffId())
                .correspondentsId(receipt.getCustomerId())
                .correspondentsName(receipt.getCustomerName())
                .operatorName(receipt.getOrderStaffName())
                .remarks(targetStatus == OrderStatus.已审核 ? "其他收入单审核通过" : "其他收入单反审核")
                .build();

        if (targetStatus == OrderStatus.已审核) {
            context.setAmount(amount);
        } else {
            context.setAmount(amount.negate());
        }

        accountService.updateAccountBalanceWithFlow(context);
    }

    private static @NotNull CustomerFlow getCustomerFlow(OtherReceipt receipt, OrderStatus targetStatus, Customer customer) {
        CustomerFlow.CustomerFlowType flowType;
        CustomerFlow customerFlow = new CustomerFlow();
        customerFlow.setCustomerId(customer.getId());
        customerFlow.setBusinessId(receipt.getId());
        customerFlow.setBusinessNo(receipt.getOrderNo());
        customerFlow.setBusinessDate(receipt.getOrderDate());
        BigDecimal amount = receipt.getCollectionAmount();
        if (targetStatus == OrderStatus.已审核) {
            flowType = CustomerFlow.CustomerFlowType.其他收入单;
            customerFlow.setPaidUpAmount(amount);
        } else {
            flowType = CustomerFlow.CustomerFlowType.反审核_其他收入单;
            customerFlow.setPaidUpAmount(amount != null ? amount.negate() : BigDecimal.ZERO);
        }
        customerFlow.setCustomerFlowType(flowType);
        customerFlow.setBalanceReceivables(customer.getBalance());
        customerFlow.setAccountBookId(receipt.getAccountBookId());
        customerFlow.setMerchantId(receipt.getMerchantId());
        customerFlow.setCreatedBy(receipt.getApprovedBy());
        customerFlow.setCreatedAt(LocalDateTime.now());
        customerFlow.setRemarks(targetStatus == OrderStatus.已审核 ? "其他收入单审核通过" : "其他收入单反审核");
        return customerFlow;
    }

    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qOtherReceipt)
                .select(qOtherReceipt.collectionAmount.sum())
                .where(query.builder).fetchFirst();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherReceipt.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherReceipt.accountBookId.eq(accountBookId));
            }
        }

    }
}
