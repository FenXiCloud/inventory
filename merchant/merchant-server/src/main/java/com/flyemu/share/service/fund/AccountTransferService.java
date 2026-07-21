package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.basic.Account;
import com.flyemu.share.entity.basic.QAccount;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.entity.setting.QMerchantUser;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.AccountRepository;
import com.flyemu.share.repository.AccountTransferItemRepository;
import com.flyemu.share.repository.AccountTransferRepository;
import com.flyemu.share.service.setting.CheckoutService;
import com.flyemu.share.service.AbsService;
import com.flyemu.share.service.basic.AccountService;
import com.flyemu.share.service.fund.dto.AccountBalanceChangeContext;
import com.flyemu.share.form.AccountTransferForm;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.vo.AccountTransferDetails;
import com.flyemu.share.service.fund.vo.AccountTransferDetailsVO;
import com.flyemu.share.service.fund.vo.AccountTransferQueryVO;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.way.CodeGenerator;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAUpdateClause;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * @功能描述: 资金转账
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountTransferService extends AbsService {

    private final CheckoutService checkoutService;
    private final static QAccountTransfer qAccountTransfer = QAccountTransfer.accountTransfer;
    private final static QAccount qAccount = QAccount.account;
    private final static QAccountTransferItem qAccountTransferItem = QAccountTransferItem.accountTransferItem;
    private final AccountService accountService;
    private final AccountTransferRepository accountTransferRepository;
    private final CodeRuleService codeRuleService;
    private final AccountRepository accountRepository;
    private final AccountTransferItemRepository accountTransferItemRepository;

    public PageResults<AccountTransferQueryVO> query(Page page, AccountTransferService.Query query) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        JPAQuery<AccountTransferQueryVO> mainQuery = jqf.select(
                        Projections.bean(AccountTransferQueryVO.class,
                                qAccountTransfer.id,
                                qAccountTransfer.orderDate,
                                qAccountTransfer.remarks,
                                qAccountTransfer.orderNo,
                                qAccountTransfer.amount,
                                qAccountTransfer.orderStatus,
                                qAccountTransfer.approvedBy,
                                qAccountTransfer.approvedAt,
                                qAccountTransfer.createdBy,
                                qAccountTransfer.createdAt,
                                qAccountTransfer.accountBookId,
                                qAccountTransfer.merchantId,
                                qCreatedByUser.name.as("createName"),
                                qApprovedByUser.name.as("approvedName")
                        ))
                .from(qAccountTransfer)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qAccountTransfer.createdBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qAccountTransfer.approvedBy))
                .where(query.builder);

        List<AccountTransferQueryVO> mainList = mainQuery.offset(page.getOffset())
                .limit(page.getPageSize())
                .fetch();

        List<Long> transferIds = mainList.stream()
                .map(AccountTransferDetailsVO::getId)
                .toList();

        Map<Long, List<AccountTransferItem>> itemMap = new HashMap<>();
        if (!transferIds.isEmpty()) {
            List<AccountTransferItem> allItems = jqf.select(qAccountTransferItem)
                    .from(qAccountTransferItem)
                    .where(qAccountTransferItem.accountTransferId.in(transferIds))
                    .fetch();

            for (AccountTransferItem item : allItems) {
                itemMap.computeIfAbsent(item.getAccountTransferId(), k -> new ArrayList<>()).add(item);
            }
        }

        for (AccountTransferQueryVO vo : mainList) {
            vo.setItemList(itemMap.getOrDefault(vo.getId(), Collections.emptyList()));
        }

        long total = mainQuery.fetchCount();
        return new PageResults<>(mainList, page, total);
    }

    @Transactional
    public AccountTransfer save(AccountTransferForm dto) {
        AccountTransfer accountTransfer = dto.getOrder();
        if (accountTransfer != null) {
            checkoutService.assertEditable(accountTransfer.getMerchantId(), accountTransfer.getAccountBookId(), accountTransfer.getOrderDate());
        }
        List<AccountTransferItem> items = dto.getItemList();

        if (accountTransfer == null) {
            throw new ServiceException("参数错误");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (AccountTransferItem item : items) {
            if (item.getAmount() == null || item.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("明细金额必须大于0");
            }
            if (item.getFromAccountId() == null || item.getToAccountId() == null) {
                throw new ServiceException("转出或转入账户不能为空");
            }
            Account fromAccount = accountRepository.findById(item.getFromAccountId())
                    .orElseThrow(() -> new ServiceException("转出账户不存在"));
            Account toAccount = accountRepository.findById(item.getFromAccountId())
                    .orElseThrow(() -> new ServiceException("转出账户不存在"));

            if (item.getAmount().compareTo(fromAccount.getBalance()) > 0) {
                throw new ServiceException("明细金额超过【" + fromAccount.getName() + "】账户余额");
            }
            if (item.getFromAccountId().equals(item.getToAccountId())) {
                throw new ServiceException("转出账户与转入账户不能相同：" + fromAccount.getName());
            }

            if (!fromAccount.getEnabled()) {
                throw new ServiceException("转出账户已被禁用：" + fromAccount.getName());
            }
            if (!toAccount.getEnabled()) {
                throw new ServiceException("转入账户已被禁用：" + toAccount.getName());
            }

            totalAmount = totalAmount.add(item.getAmount());
        }
        accountTransfer.setAmount(totalAmount);
        if (accountTransfer.getId() == null) {
            accountTransfer.setCreatedAt(LocalDateTime.now());
            if (accountTransfer.getOrderStatus() == null) {
                accountTransfer.setOrderStatus(OrderStatus.已保存);
            }
            assignOrderNumber(accountTransfer);
            AccountTransfer saved = accountTransferRepository.save(accountTransfer);

            if (!items.isEmpty()) {
                for (AccountTransferItem item : items) {
                    item.setAccountBookId(saved.getAccountBookId());
                    item.setMerchantId(saved.getMerchantId());
                    item.setAccountTransferId(saved.getId());
                }
                accountTransferItemRepository.saveAll(items);
            }

            return saved;

        } else {

            AccountTransfer original = accountTransferRepository.findById(accountTransfer.getId())
                    .orElseThrow(() -> new ServiceException("转账单不存在"));

            if (!OrderStatus.已保存.equals(original.getOrderStatus())) {
                throw new ServiceException("该单据不是【已保存】状态，无法修改");
            }

            jqf.delete(QAccountTransferItem.accountTransferItem)
                    .where(QAccountTransferItem.accountTransferItem.accountTransferId.eq(original.getId()))
                    .execute();

            BeanUtil.copyProperties(accountTransfer, original, CopyOptions.create().ignoreNullValue());

            for (AccountTransferItem item : items) {
                item.setAccountBookId(original.getAccountBookId());
                item.setMerchantId(original.getMerchantId());
                item.setAccountTransferId(original.getId());
            }
            accountTransferItemRepository.saveAll(items);
            if (accountTransfer.getOrderStatus() == OrderStatus.已审核) {
                updateAccountBalancesWithFlow(original, OrderStatus.已审核);
            }
            return accountTransferRepository.save(original);
        }
    }

    private void assignOrderNumber(AccountTransfer transfer) {
        if (StringUtils.isNotBlank(transfer.getOrderNo())) {
            return;
        }

        CodeRule codeRule = codeRuleService.findByDocumentTypeAndMerchantIdAndAccountBookId(
                CodeRule.DocumentType.转帐单,
                transfer.getMerchantId(),
                transfer.getAccountBookId());

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
                Long count = jqf.select(qAccountTransfer.id.count())
                        .from(qAccountTransfer)
                        .where(qAccountTransfer.merchantId.eq(transfer.getMerchantId())
                                .and(qAccountTransfer.accountBookId.eq(transfer.getAccountBookId())))
                        .fetchOne();

                Integer currentSerial = Math.toIntExact(count != null ? count + 1 : 1L);
                String serialStr = String.format("%0" + serialLength + "d", currentSerial);
                codeBuilder.append(serialStr);
            }
        } else {
            codeBuilder.append(CodeGenerator.generateCode());
        }

        transfer.setOrderNo(codeBuilder.toString());
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

        List<AccountTransfer> transferList = jqf.select(qAccountTransfer)
                .from(qAccountTransfer)
                .where(qAccountTransfer.id.in(idList)
                        .and(qAccountTransfer.merchantId.eq(merchantId))
                        .and(qAccountTransfer.accountBookId.eq(accountBookId)))
                .fetch();

        for (AccountTransfer transfer : transferList) {
            if (!OrderStatus.已保存.equals(transfer.getOrderStatus())) {
                throw new ServiceException("只能删除【已保存】状态的转账单：" + transfer.getOrderNo());
            }
        }
        jqf.delete(qAccountTransferItem)
                .where(qAccountTransferItem.accountTransferId.in(idList))
                .execute();
        long count = jqf.delete(qAccountTransfer)
                .where(qAccountTransfer.id.in(idList)
                        .and(qAccountTransfer.merchantId.eq(merchantId))
                        .and(qAccountTransfer.accountBookId.eq(accountBookId)))
                .execute();
        if (count == 0) {
            throw new ServiceException("删除失败");
        }
    }

    public AccountTransferDetails load(Long merchantId, Long id) {
        QMerchantUser qCreatedByUser = new QMerchantUser("createdByUser");
        QMerchantUser qUpdatedByUser = new QMerchantUser("updatedByUser");
        QMerchantUser qApprovedByUser = new QMerchantUser("approvedByUser");

        AccountTransferDetailsVO vo = jqf.select(
                        Projections.bean(AccountTransferDetailsVO.class,
                                qAccountTransfer.id,
                                qAccountTransfer.orderDate,
                                qAccountTransfer.orderNo,
                                qAccountTransfer.amount,
                                qAccountTransfer.orderStatus,
                                qAccountTransfer.approvedBy,
                                qAccountTransfer.approvedAt,
                                qAccountTransfer.createdBy,
                                qAccountTransfer.createdAt,
                                qAccountTransfer.updateBy,
                                qAccountTransfer.updateAt,
                                qAccountTransfer.accountBookId,
                                qAccountTransfer.merchantId,
                                qAccountTransfer.remarks,
                                qCreatedByUser.name.as("createName"),
                                qUpdatedByUser.name.as("updateName"),
                                qApprovedByUser.name.as("approvedName")
                        ))
                .from(qAccountTransfer)
                .leftJoin(qCreatedByUser).on(qCreatedByUser.id.eq(qAccountTransfer.createdBy))
                .leftJoin(qUpdatedByUser).on(qUpdatedByUser.id.eq(qAccountTransfer.updateBy))
                .leftJoin(qApprovedByUser).on(qApprovedByUser.id.eq(qAccountTransfer.approvedBy))
                .where(qAccountTransfer.merchantId.eq(merchantId).and(qAccountTransfer.id.eq(id)))
                .fetchOne();

        if (vo == null) {
            throw new ServiceException("转账单不存在");
        }
        AccountTransferDetails accountTransferDetails = new AccountTransferDetails();
        accountTransferDetails.setOrder(vo);
        List<AccountTransferItem> items = jqf.select(qAccountTransferItem)
                .from(qAccountTransferItem)
                .where(qAccountTransferItem.accountTransferId.eq(id))
                .fetch()
                .stream()
                .distinct()
                .toList();
        accountTransferDetails.setItemList(items);
        return accountTransferDetails;
    }

    /**
     * 审核转账单
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
        if (dto.getApprovedBy() == null && targetStatus == OrderStatus.已审核) {
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

        List<AccountTransfer> transferList = jqf.selectFrom(qAccountTransfer)
                .where(qAccountTransfer.id.in(idList))
                .fetch();

        LocalDateTime now = LocalDateTime.now();

        for (AccountTransfer transfer : transferList) {
            if (targetStatus == OrderStatus.已审核 && !OrderStatus.已保存.equals(transfer.getOrderStatus())) {
                throw new ServiceException("只能审核【已保存】状态的单据：" + transfer.getOrderNo());
            }
            if (targetStatus == OrderStatus.已保存 && !OrderStatus.已审核.equals(transfer.getOrderStatus())) {
                throw new ServiceException("只能反审核【已审核】状态的单据：" + transfer.getOrderNo());
            }

            transfer.setOrderStatus(targetStatus);
            if (targetStatus == OrderStatus.已审核) {
                validateTransferItemsBeforeApprove(transfer);
                transfer.setApprovedAt(now);
                transfer.setApprovedBy(dto.getApprovedBy());
            } else {
                validateTransferItemsBeforeUnApprove(transfer);
                transfer.setApprovedAt(null);
                transfer.setApprovedBy(null);
            }
            accountTransferRepository.save(transfer);
            updateAccountBalancesWithFlow(transfer, targetStatus);
        }

        jqf.update(qAccountTransfer)
                .set(qAccountTransfer.orderStatus, targetStatus)
                .set(qAccountTransfer.approvedAt, targetStatus == OrderStatus.已审核 ? now : null)
                .set(qAccountTransfer.approvedBy, targetStatus == OrderStatus.已审核 ? dto.getApprovedBy() : null)
                .where(qAccountTransfer.id.in(idList))
                .execute();
    }

    private void validateTransferItemsBeforeApprove(AccountTransfer transfer) {
        List<AccountTransferItem> items = findItemsByTransferId(transfer.getId());

        if (items == null || items.isEmpty()) {
            throw new ServiceException("该转账单没有明细");
        }

        for (AccountTransferItem item : items) {
            Account fromAccount = accountRepository.findById(item.getFromAccountId())
                    .orElseThrow(() -> new ServiceException("转出账户不存在：" + item.getFromAccountId()));

            Account toAccount = accountRepository.findById(item.getToAccountId())
                    .orElseThrow(() -> new ServiceException("转入账户不存在：" + item.getToAccountId()));

            if (!fromAccount.getEnabled()) {
                throw new ServiceException("转出账户【" + fromAccount.getName() + "】已被禁用");
            }
            if (!toAccount.getEnabled()) {
                throw new ServiceException("转入账户【" + toAccount.getName() + "】已被禁用");
            }

            if (item.getFromAccountId().equals(item.getToAccountId())) {
                throw new ServiceException("转出账户与转入账户不能相同：" + fromAccount.getName());
            }
            BigDecimal currentBalance = fromAccount.getBalance();
            if (item.getAmount().compareTo(currentBalance) > 0) {
                throw new ServiceException("转出账户【" + fromAccount.getName() + "】余额已不足");
            }
        }
    }

    private void validateTransferItemsBeforeUnApprove(AccountTransfer transfer) {
        List<AccountTransferItem> items = findItemsByTransferId(transfer.getId());

        if (items == null || items.isEmpty()) {
            throw new ServiceException("该转账单没有明细");
        }

        for (AccountTransferItem item : items) {
            Account toAccount = accountRepository.findById(item.getToAccountId())
                    .orElseThrow(() -> new ServiceException("转入账户不存在：" + item.getToAccountId()));
            if (toAccount.getBalance().compareTo(item.getAmount()) < 0) {
                throw new ServiceException("转入账户【" + toAccount.getName() + "】余额不足，无法完成反审核");
            }
        }
    }

    @Transactional
    public void updateAccountBalancesWithFlow(AccountTransfer transfer, OrderStatus targetStatus) {
        List<AccountTransferItem> items = findItemsByTransferId(transfer.getId());
        for (AccountTransferItem item : items) {
            BigDecimal amount = item.getAmount();
            if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            Long merchantId = transfer.getMerchantId();
            Long accountBookId = transfer.getAccountBookId();
            Long voucherId = transfer.getId();
            String businessNo = transfer.getOrderNo();
            Long operatorId =null;

            Long fromAccountId = item.getFromAccountId();
            Long toAccountId = item.getToAccountId();

            if (fromAccountId.equals(toAccountId)) {
                throw new ServiceException("转出账户与转入账户不能相同");
            }


            if (targetStatus == OrderStatus.已审核) {
                AccountBalanceChangeContext outContext = AccountBalanceChangeContext.builder()
                        .accountId(fromAccountId)
                        .merchantId(merchantId)
                        .accountBookId(accountBookId)
                        .voucherId(voucherId)
                        .businessNo(businessNo)
                        .flowType(AccountFlow.AccountFlowType.资金转账)
                        .operatorId(operatorId)
                        .remarks("转账单审核通过 - 转出")
                        .build();
                outContext.setAmount(amount.negate());
                accountService.updateAccountBalanceWithFlow(outContext);

                AccountBalanceChangeContext inContext = AccountBalanceChangeContext.builder()
                        .accountId(toAccountId)
                        .merchantId(merchantId)
                        .accountBookId(accountBookId)
                        .voucherId(voucherId)
                        .businessNo(businessNo)
                        .flowType(AccountFlow.AccountFlowType.资金转账)
                        .operatorId(operatorId)
                        .remarks("转账单审核通过 - 转入")
                        .build();
                inContext.setAmount(amount);
                accountService.updateAccountBalanceWithFlow(inContext);
            } else {
                AccountBalanceChangeContext rollbackOutContext = AccountBalanceChangeContext.builder()
                        .accountId(fromAccountId)
                        .merchantId(merchantId)
                        .accountBookId(accountBookId)
                        .voucherId(voucherId)
                        .businessNo(businessNo)
                        .flowType(AccountFlow.AccountFlowType.资金转账)
                        .operatorId(operatorId)
                        .remarks("转账单反审核 - 回滚转出")
                        .build();
                rollbackOutContext.setAmount(amount);
                accountService.updateAccountBalanceWithFlow(rollbackOutContext);

                AccountBalanceChangeContext rollbackInContext = AccountBalanceChangeContext.builder()
                        .accountId(toAccountId)
                        .merchantId(merchantId)
                        .accountBookId(accountBookId)
                        .voucherId(voucherId)
                        .businessNo(businessNo)
                        .flowType(AccountFlow.AccountFlowType.资金转账)
                        .operatorId(operatorId)
                        .remarks("转账单反审核 - 回滚转入")
                        .build();
                rollbackInContext.setAmount(amount.negate());
                accountService.updateAccountBalanceWithFlow(rollbackInContext);
            }
        }
    }


    private List<AccountTransferItem> findItemsByTransferId(Long transferId) {
        return jqf.select(qAccountTransferItem)
                .from(qAccountTransferItem)
                .where(qAccountTransferItem.accountTransferId.eq(transferId))
                .fetch();
    }


    public BigDecimal queryTotal(Query query) {
        return bqf.selectFrom(qAccountTransfer)
                .select(qAccountTransfer.amount.sum())
                .where(query.builder).fetchFirst();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAccountTransfer.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qAccountTransfer.accountBookId.eq(accountBookId));
            }
        }
//        }
    }
}
