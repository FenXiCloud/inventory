package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.OtherExpenseItemRepository;
import com.flyemu.share.repository.OtherExpenseRepository;
import com.flyemu.share.repository.SupplierRepository;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.basic.SupplierService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.vo.OtherExpenseDetails;
import com.flyemu.share.service.fund.vo.OtherExpenseDetailsVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @功能描述: 其他支出单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OtherExpenseService extends AbsService {

    private final static QOtherExpense qOtherExpense = QOtherExpense.otherExpense;
    private final static QOtherExpenseItem qotherExpenseItem = QOtherExpenseItem.otherExpenseItem;

    private final OtherExpenseRepository otherExpenseRepository;
    private final OtherExpenseItemRepository otherExpenseItemRepository;
    private final SupplierService supplierService;
    private final CodeRuleService codeRuleService;

    private final AccountService accountService;

    public PageResults<OtherExpenseDetailsVO> query(Page page, OtherExpenseService.Query query) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        JPAQuery<OtherExpenseDetailsVO> mainQuery = jqf.select(Projections.bean(OtherExpenseDetailsVO.class,
                        qOtherExpense.id,
                        qOtherExpense.orderNo,
                        qOtherExpense.orderDate,

                        qOtherExpense.settlementAccount,
                        qOtherExpense.settlementAccountId,
                        qOtherExpense.orderStaffId,
                        qOtherExpense.orderStaffName,
                        qOtherExpense.collectionAmount,
                        qOtherExpense.supplierId,
                        qOtherExpense.supplierName,
                        qOtherExpense.arrearsAmount,
                        qOtherExpense.orderStatus,
                        qOtherExpense.approvedAt,
                        qOtherExpense.approvedBy,
                        qOtherExpense.createdBy,
                        qOtherExpense.createdAt,
                        qOtherExpense.updateAt,
                        qOtherExpense.accountBookId,
                        qOtherExpense.merchantId,
                        qCreatedByUser.name.as("createName"),
                        qApprovedByUser.name.as("approvedName")
                ))
                .from(qOtherExpense)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOtherExpense.createdBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOtherExpense.approvedBy))
                .where(query.builder);

        List<OtherExpenseDetailsVO> mainList = mainQuery.offset(page.getOffset()).limit(page.getPageSize()).fetch();
        long total = mainQuery.fetchCount();

        return new PageResults<>(mainList, page, total);
    }

    @Transactional
    public OtherExpense save(OtherExpense otherExpense, List<OtherExpenseItem> items) {
        if (otherExpense == null) {
            throw new ServiceException("参数错误");
        }

        if (otherExpense.getId() == null) {
            otherExpense.setCreatedAt(LocalDateTime.now());
            if (otherExpense.getOrderStatus() == null) {
                otherExpense.setOrderStatus(OrderStatus.已保存);
            }
            assignOrderNumber(otherExpense);
        } else {
            OtherExpense original = otherExpenseRepository.findById(otherExpense.getId())
                    .orElseThrow(() -> new ServiceException("其他支出单不存在"));
            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }
            jqf.delete(qotherExpenseItem)
                    .where(qotherExpenseItem.otherExpenseId.eq(otherExpense.getId()))
                    .execute();
        }

        otherExpense = otherExpenseRepository.save(otherExpense);

        if (items != null && !items.isEmpty()) {
            for (OtherExpenseItem item : items) {
                item.setOtherExpenseId(otherExpense.getId());
                item.setMerchantId(otherExpense.getMerchantId());
                item.setAccountBookId(otherExpense.getAccountBookId());
                otherExpenseItemRepository.save(item);
            }
        }

        if (OrderStatus.已审核.equals(otherExpense.getOrderStatus())) {
            updateSupplierBalance(otherExpense);
        }

        return otherExpense;
    }

    private void assignOrderNumber(OtherExpense expense) {
        if (StringUtils.isNotBlank(expense.getOrderNo())) {
            return;
        }

        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.其他付款单,
                expense.getMerchantId(),
                expense.getAccountBookId());

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
                Long count = jqf.select(qOtherExpense.id.count())
                        .from(qOtherExpense)
                        .where(qOtherExpense.merchantId.eq(expense.getMerchantId())
                                .and(qOtherExpense.accountBookId.eq(expense.getAccountBookId())))
                        .fetchOne();

                Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                codeBuilder.append(serialStr);
            }
        } else {
            codeBuilder.append(CodeGenerator.generateCode());
        }

        expense.setOrderNo(codeBuilder.toString());
    }

    private void updateSupplierBalance(OtherExpense expense) {
        if (expense.getApprovedBy() == null) {
            throw new ServiceException("已审核状态,审核人必填");
        }
        expense.setApprovedAt(LocalDateTime.now());
        updateSupplierAndAccountBalances(expense, OrderStatus.已审核);
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

        List<OtherExpense> expenseList = jqf.selectFrom(qOtherExpense)
                .where(qOtherExpense.id.in(idList)
                        .and(qOtherExpense.merchantId.eq(merchantId))
                        .and(qOtherExpense.accountBookId.eq(accountBookId)))
                .fetch();

        if (expenseList.isEmpty()) {
            throw new ServiceException("没有找到可删除的其他支出单");
        }

        for (OtherExpense expense : expenseList) {
            if (!OrderStatus.已保存.equals(expense.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的单据：" + expense.getOrderNo());
            }
        }

        jqf.delete(qotherExpenseItem)
                .where(qotherExpenseItem.otherExpenseId.in(idList))
                .execute();

        jqf.delete(qOtherExpense)
                .where(qOtherExpense.id.in(idList)
                        .and(qOtherExpense.merchantId.eq(merchantId))
                        .and(qOtherExpense.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherExpense> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherExpense).where(qOtherExpense.merchantId.eq(merchantId).and(qOtherExpense.accountBookId.eq(accountBookId))).fetch();
    }

    public OtherExpenseDetails selectById(Long id) {
        if (id == null || id <= 0) {
            throw new ServiceException("ID不能为空");
        }

        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        OtherExpenseDetailsVO otherExpenseVO = jqf.select(Projections.bean(OtherExpenseDetailsVO.class,
                        qOtherExpense.id,
                        qOtherExpense.supplierId,
                        qOtherExpense.settlementAccount,
                        qOtherExpense.settlementAccountId,
                        qOtherExpense.supplierId,
                        qOtherExpense.supplierName,
                        qOtherExpense.orderStaffId,
                        qOtherExpense.orderStaffName,
                        qOtherExpense.orderDate,
                        qOtherExpense.orderNo,
                        qOtherExpense.collectionAmount,
                        qOtherExpense.arrearsAmount,
                        qOtherExpense.orderStatus,
                        qOtherExpense.approvedAt,
                        qOtherExpense.approvedBy,
                        qOtherExpense.createdBy,
                        qOtherExpense.createdAt,
                        qOtherExpense.updateBy,
                        qOtherExpense.updateAt,
                        qOtherExpense.accountBookId,
                        qOtherExpense.merchantId,
                        qCreatedByUser.name.as("createName"),
                        qUpdatedByUser.name.as("updaterName"),
                        qApprovedByUser.name.as("approverName")
                ))
                .from(qOtherExpense)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qOtherExpense.createdBy))
                .leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qOtherExpense.updateBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qOtherExpense.approvedBy))
                .where(qOtherExpense.id.eq(id))
                .fetchOne();

        if (otherExpenseVO == null) {
            throw new ServiceException("单据不存在");
        }

        List<OtherExpenseItem> items = jqf.select(qotherExpenseItem)
                .from(qotherExpenseItem)
                .where(qotherExpenseItem.otherExpenseId.eq(id))
                .fetch();

        OtherExpenseDetails details = new OtherExpenseDetails();
        details.setOrder(otherExpenseVO);
        details.setItemList(items);

        return details;
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

        List<OtherExpense> expenseList = jqf.selectFrom(qOtherExpense)
                .where(qOtherExpense.id.in(idList))
                .fetch();

        LocalDateTime now = LocalDateTime.now();

        for (OtherExpense expense : expenseList) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(expense.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + expense.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(expense.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + expense.getOrderNo());
            }
            updateSupplierAndAccountBalances(expense, targetStatus);
            expense.setOrderStatus(targetStatus);
            if (targetStatus == OrderStatus.已审核) {
                expense.setApprovedAt(now);
                expense.setApprovedBy(dto.getApprovedBy());
            } else {
                expense.setApprovedAt(null);
                expense.setApprovedBy(null);
            }
            otherExpenseRepository.save(expense);

        }

        jqf.update(qOtherExpense)
                .set(qOtherExpense.orderStatus, targetStatus)
                .set(qOtherExpense.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qOtherExpense.approvedBy, targetStatus == OrderStatus.已审核 ? dto.getApprovedBy() : null)
                .where(qOtherExpense.id.in(idList))
                .execute();
    }
    @Transactional
    public void updateSupplierAndAccountBalances(OtherExpense expense, OrderStatus targetStatus) {
        Supplier supplier = supplierService.selectByPrimaryKey(expense.getSupplierId());

        BigDecimal amount = expense.getCollectionAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) < 0) {
            amount = BigDecimal.ZERO;
        }


        SupplierFlow.SupplierFlowType flowType;
        if (targetStatus == OrderStatus.已审核) {
            flowType = SupplierFlow.SupplierFlowType.其他支出单;
            supplier.setBalance(supplier.getBalance().subtract(amount));
        } else {
            if (expense.getOrderStatus() != OrderStatus.已审核) {
                throw new ServiceException("只有已审核的单据才能反审核");
            }
            flowType = SupplierFlow.SupplierFlowType.反审核_其他支出单;
            supplier.setBalance(supplier.getBalance().add(amount));
        }

        SupplierFlow supplierFlow = new SupplierFlow();
        supplierFlow.setSupplierId(supplier.getId());
        supplierFlow.setBusinessId(expense.getId());
        supplierFlow.setBusinessNo(expense.getOrderNo());
        supplierFlow.setSupplierFlowType(flowType);
        supplierFlow.setPurchaseAmount(amount);
        supplierFlow.setCopeWithAmount(amount);
        supplierFlow.setActualPaymentAmount(targetStatus == OrderStatus.已审核 ? amount : amount.negate());
        supplierFlow.setBalancePayable(supplier.getBalance());
        supplierFlow.setAccountBookId(expense.getAccountBookId());
        supplierFlow.setMerchantId(expense.getMerchantId());
        supplierFlow.setCreatedBy(expense.getApprovedBy());
        supplierFlow.setCreatedAt(LocalDateTime.now());
        supplierFlow.setRemarks(targetStatus == OrderStatus.已审核 ? "其他支出单审核通过" : "其他支出单反审核");
        supplierFlow.setBusinessDate(expense.getOrderDate());
        supplierService.updateTheBalance(supplier,supplierFlow);

        Long settlementAccountId = expense.getSettlementAccountId();
        if (settlementAccountId==null) {
            throw new ServiceException("结算账户不能为空");
        }

        BigDecimal paymentAmount = amount;
        AccountBalanceChangeContext context = AccountBalanceChangeContext.builder()
                .accountId(settlementAccountId)
                .merchantId(expense.getMerchantId())
                .accountBookId(expense.getAccountBookId())
                .voucherId(expense.getId())
                .supplierId(expense.getSupplierId())
                .businessNo(expense.getOrderNo())
                .flowType(AccountFlow.AccountFlowType.其他支出单)
                .operatorId(expense.getOrderStaffId())
                .correspondentsId(expense.getSupplierId())
                .correspondentsName(expense.getSupplierName())
                .operatorName(expense.getOrderStaffName())
                .remarks(targetStatus == OrderStatus.已审核 ? "其他支出单审核通过" : "其他支出单反审核")
                .build();

        if (targetStatus == OrderStatus.已审核) {
            context.setAmount(paymentAmount);
        } else {
            context.setAmount(paymentAmount.negate());
        }

        accountService.updateAccountBalanceWithFlow(context);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherExpense.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherExpense.accountBookId.eq(accountBookId));
            }
        }
    }
}
