package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.QCustomer;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.CustomerRepository;
import com.flyemu.share.repository.OtherIncomeItemRepository;
import com.flyemu.share.repository.OtherIncomeRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.basic.CustomerService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.dto.OtherIncomeDTO;
import com.flyemu.share.service.fund.vo.OtherIncomeDetails;
import com.flyemu.share.service.fund.vo.OtherIncomeDetailsVO;
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
@Slf4j
@RequiredArgsConstructor
public class OtherIncomeService extends AbsService {

    private final static QOtherIncome qOtherIncome = QOtherIncome.otherIncome;
    private final static QOtherIncomeItem qotherIncomeItem = QOtherIncomeItem.otherIncomeItem;
    private final OtherIncomeItemRepository otherIncomeItemRepository;
    private final CustomerService customerService;
    private final OtherIncomeRepository otherIncomeRepository;
    private final CodeRuleService codeRuleService;
    private final AccountService accountService;

    public PageResults<OtherIncomeDetailsVO> query(Page page, OtherIncomeService.Query query) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");
        QCustomer qCustomer = QCustomer.customer;

        JPAQuery<OtherIncomeDetailsVO> mainQuery = jqf.select(Projections.bean(OtherIncomeDetailsVO.class,
                        qOtherIncome.id,
                        qOtherIncome.orderNo,
                        qOtherIncome.orderDate,
                        qOtherIncome.collectionAmount,
                        qOtherIncome.customerId,
                        qOtherIncome.customerName,
                        qOtherIncome.orderStaffId,
                        qOtherIncome.orderStaffName,
                        qOtherIncome.expirationDate,
                        qOtherIncome.arrearsAmount,
                        qOtherIncome.orderStatus,
                        qOtherIncome.approvedAt,
                        qOtherIncome.approvedBy,
                        qOtherIncome.createdBy,
                        qOtherIncome.createdAt,
                        qOtherIncome.updateAt,
                        qOtherIncome.accountBookId,
                        qOtherIncome.merchantId,
                        qCreatedByUser.name.as("createName"),
                        qApprovedByUser.name.as("approvedName")
                ))
                .from(qOtherIncome)
                .leftJoin(qCustomer).on(qCustomer.id.eq(qOtherIncome.customerId))
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOtherIncome.createdBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOtherIncome.approvedBy))
                .where(query.builder);


        List<OtherIncomeDetailsVO> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        long total = mainQuery.fetchCount();

        return new PageResults<>(mainList, page, total);
    }


    @Transactional
    public OtherIncome save(OtherIncomeDTO dto) {
        OtherIncome otherIncome = dto.getOrder();
        List<OtherIncomeItem> items = dto.getItemList();
        if (otherIncome == null) {
            throw new ServiceException("参数错误");
        }
        if (otherIncome.getCollectionAmount() == null || otherIncome.getCollectionAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("收款金额必须大于0");
        }

        if (items != null && !items.isEmpty()) {
            for (OtherIncomeItem item : items) {
                if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ServiceException("明细项金额必须大于0");
                }
            }
        }
        if (otherIncome.getCustomerId() == null) {
            throw new ServiceException("请选择客户");
        }
        if (OrderStatus.已审核.equals(otherIncome.getOrderStatus())) {
            if (otherIncome.getApprovedBy() == null) {
                throw new ServiceException("已审核状态,审核人必填");
            }
            otherIncome.setApprovedAt(LocalDateTime.now());
        }
        if (otherIncome.getId() == null) {
            otherIncome.setCreatedAt(LocalDateTime.now());
            if (otherIncome.getOrderStatus() == null) {
                otherIncome.setOrderStatus(OrderStatus.已保存);
            }
            assignOrderNumber(otherIncome);
        } else {
            OtherIncome original = otherIncomeRepository.findById(otherIncome.getId())
                    .orElseThrow(() -> new ServiceException("其他收入单不存在"));
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            jqf.delete(qotherIncomeItem)
                    .where(qotherIncomeItem.otherIncomeId.eq(otherIncome.getId()))
                    .execute();
        }


        otherIncome = otherIncomeRepository.save(otherIncome);
        if (items != null && !items.isEmpty()) {
            for (OtherIncomeItem item : items) {
                item.setOtherIncomeId(otherIncome.getId());
                item.setMerchantId(otherIncome.getMerchantId());
                item.setAccountBookId(otherIncome.getAccountBookId());
                otherIncomeItemRepository.save(item);
            }
        }
        if (OrderStatus.已审核.equals(otherIncome.getOrderStatus())) {
            updateCustomerBalance(otherIncome);
        }
        return otherIncome;
    }

    private void assignOrderNumber(OtherIncome income) {
        if (StringUtils.isNotBlank(income.getOrderNo())) {
            return;
        }

        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.其他收款单,
                income.getMerchantId(),
                income.getAccountBookId());

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
                Long count = jqf.select(qOtherIncome.id.count())
                        .from(qOtherIncome)
                        .where(qOtherIncome.merchantId.eq(income.getMerchantId())
                                .and(qOtherIncome.accountBookId.eq(income.getAccountBookId())))
                        .fetchOne();

                Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                codeBuilder.append(serialStr);
            }
        } else {
            codeBuilder.append(CodeGenerator.generateCode());
        }

        income.setOrderNo(codeBuilder.toString());
    }

    @Transactional
    private void updateCustomerBalance(OtherIncome otherIncome) {
        if (otherIncome.getApprovedBy() == null) {
            throw new ServiceException("已审核状态,审核人必填");
        }
        otherIncome.setApprovedAt(LocalDateTime.now());
        updateCustomerAndAccountBalances(otherIncome, OrderStatus.已审核);
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

        List<OtherIncome> incomeList = jqf.selectFrom(qOtherIncome)
                .where(qOtherIncome.id.in(idList)
                        .and(qOtherIncome.merchantId.eq(merchantId))
                        .and(qOtherIncome.accountBookId.eq(accountBookId)))
                .fetch();

        if (incomeList.isEmpty()) {
            throw new ServiceException("没有找到可删除的其他收入单");
        }

        for (OtherIncome income : incomeList) {
            if (!OrderStatus.已保存.equals(income.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的单据：" + income.getOrderNo());
            }
        }

        jqf.delete(qotherIncomeItem)
                .where(qotherIncomeItem.otherIncomeId.in(idList))
                .execute();

        jqf.delete(qOtherIncome)
                .where(qOtherIncome.id.in(idList)
                        .and(qOtherIncome.merchantId.eq(merchantId))
                        .and(qOtherIncome.accountBookId.eq(accountBookId)))
                .execute();
    }


    public List<OtherIncome> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherIncome).where(qOtherIncome.merchantId.eq(merchantId).and(qOtherIncome.accountBookId.eq(accountBookId))).fetch();
    }

    public OtherIncomeDetails selectById(Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("ID不能为空");
        }

        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        OtherIncomeDetailsVO otherIncomeVO = jqf.select(Projections.bean(OtherIncomeDetailsVO.class,
                        qOtherIncome.id,
                        qOtherIncome.customerId,
                        qOtherIncome.customerName,

                        qOtherIncome.orderStaffId,
                        qOtherIncome.orderStaffName,
                        qOtherIncome.orderDate,
                        qOtherIncome.orderNo,
                        qOtherIncome.collectionAmount,
                        qOtherIncome.arrearsAmount,
                        qOtherIncome.expirationDate,
                        qOtherIncome.orderStatus,
                        qOtherIncome.approvedAt,
                        qOtherIncome.approvedBy,
                        qOtherIncome.createdBy,
                        qOtherIncome.createdAt,
                        qOtherIncome.updateBy,
                        qOtherIncome.updateAt,
                        qOtherIncome.accountBookId,
                        qOtherIncome.merchantId,
                        qCreatedByUser.name.as("createName"),
                        qUpdatedByUser.name.as("updaterName"),
                        qApprovedByUser.name.as("approverName")
                ))
                .from(qOtherIncome)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOtherIncome.createdBy))
                .leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qOtherIncome.updateBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOtherIncome.approvedBy))
                .where(qOtherIncome.id.eq(id))
                .fetchOne();

        if (otherIncomeVO == null) {
            throw new ServiceException("单据不存在");
        }

        List<OtherIncomeItem> items = jqf.select(qotherIncomeItem)
                .from(qotherIncomeItem)
                .where(qotherIncomeItem.otherIncomeId.eq(id))
                .fetch();
        OtherIncomeDetails details = new OtherIncomeDetails();
        details.setOrder(otherIncomeVO);
        details.setItemList(items);

        return details;
    }

    /**
     * 更新单据状态（审核/反审核）
     */
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
        List<OtherIncome> incomeList = jqf.selectFrom(qOtherIncome)
                .where(qOtherIncome.id.in(idList))
                .fetch();

        if (incomeList.isEmpty()) {
            throw new ServiceException("没有找到可操作的其他收入单");
        }

        LocalDateTime now = LocalDateTime.now();

        for (OtherIncome income : incomeList) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(income.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + income.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(income.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + income.getOrderNo());
            }
            updateCustomerAndAccountBalances(income, targetStatus);
            income.setOrderStatus(targetStatus);
            if (targetStatus == OrderStatus.已审核) {
                income.setApprovedAt(now);
                income.setApprovedBy(dto.getApprovedBy());
            } else {
                income.setApprovedAt(null);
                income.setApprovedBy(null);
            }
            otherIncomeRepository.save(income);

        }

        jqf.update(qOtherIncome)
                .set(qOtherIncome.orderStatus, targetStatus)
                .set(qOtherIncome.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qOtherIncome.approvedBy, targetStatus == OrderStatus.已审核 ? dto.getApprovedBy() : null)
                .where(qOtherIncome.id.in(idList))
                .execute();
    }

    @Transactional
    public void updateCustomerAndAccountBalances(OtherIncome income, OrderStatus targetStatus) {
        Customer customer = customerService.findById(income.getCustomerId());

        BigDecimal amount = income.getCollectionAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }
        if (targetStatus == OrderStatus.已审核) {
            customer.setBalance(customer.getBalance().subtract(amount));
        } else {
            if (income.getOrderStatus() != OrderStatus.已审核) {
                throw new ServiceException("只有已审核的付款单才能反审核");
            }
            customer.setBalance(customer.getBalance().add(amount));
        }
        CustomerFlow customerFlow = getCustomerFlow(income, targetStatus, customer);
        customerService.updateTheBalance(customer, customerFlow);
        if (income.getSettlementAccountId() == null) {
            throw new ServiceException("结算账户不能为空");
        }

        AccountBalanceChangeContext context = AccountBalanceChangeContext.builder()
                .accountId(income.getSettlementAccountId())
                .merchantId(income.getMerchantId())
                .accountBookId(income.getAccountBookId())
                .voucherId(income.getId())
                .customerId(income.getCustomerId())
                .businessNo(income.getOrderNo())
                .flowType(AccountFlow.AccountFlowType.其他收入单)
                .operatorId(income.getOrderStaffId())
                .correspondentsId(income.getCustomerId())
                .correspondentsName(income.getCustomerName())
                .operatorName(income.getOrderStaffName())
                .remarks(targetStatus == OrderStatus.已审核 ? "其他收入单审核通过" : "其他收入单反审核")
                .build();

        if (targetStatus == OrderStatus.已审核) {
            context.setAmount(amount);
        } else {
            context.setAmount(amount.negate());
        }

        accountService.updateAccountBalanceWithFlow(context);
    }

    private static @NotNull CustomerFlow getCustomerFlow(OtherIncome income, OrderStatus targetStatus, Customer customer) {
        CustomerFlow.CustomerFlowType flowType;
        CustomerFlow customerFlow = new CustomerFlow();
        customerFlow.setCustomerId(customer.getId());
        customerFlow.setBusinessId(income.getId());
        customerFlow.setBusinessNo(income.getOrderNo());
        customerFlow.setBusinessDate(income.getOrderDate());
        BigDecimal amount = income.getCollectionAmount();
        if (targetStatus == OrderStatus.已审核) {
            flowType = CustomerFlow.CustomerFlowType.其他收入单;
            customerFlow.setPaidUpAmount(amount);
        } else {
            flowType = CustomerFlow.CustomerFlowType.反审核_其他收入单;
            customerFlow.setPaidUpAmount(amount != null ? amount.negate() : BigDecimal.ZERO);
        }
        customerFlow.setCustomerFlowType(flowType);
        customerFlow.setBalanceReceivables(customer.getBalance());
        customerFlow.setAccountBookId(income.getAccountBookId());
        customerFlow.setMerchantId(income.getMerchantId());
        customerFlow.setCreatedBy(income.getApprovedBy());
        customerFlow.setCreatedAt(LocalDateTime.now());
        customerFlow.setRemarks(targetStatus == OrderStatus.已审核 ? "其他收入单审核通过" : "其他收入单反审核");
        return customerFlow;
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherIncome.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherIncome.accountBookId.eq(accountBookId));
            }
        }

    }
}
