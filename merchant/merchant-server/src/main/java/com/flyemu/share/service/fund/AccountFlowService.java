package com.flyemu.share.service.fund;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.OtherFundDetailsVO;
import com.flyemu.share.entity.fund.*;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.fund.AccountFlowRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class AccountFlowService extends BaseService {

    private final static QAccountFlow qAccountFlow = QAccountFlow.accountFlow;

    private final AccountFlowRepository accountFlowRepository;

    @Transactional
    public AccountFlow save(AccountFlow accountFlow) {
        if (accountFlow.getId() != null) {
            //更新
            AccountFlow original = accountFlowRepository.getById(accountFlow.getId());
            BeanUtil.copyProperties(accountFlow, original, CopyOptions.create().ignoreNullValue());
            return accountFlowRepository.save(original);
        }
        return accountFlowRepository.save(accountFlow);
    }

    @Transactional
    public void delete(Long accountFlowId, Long merchantId, Long accountFlowBookId) {
        jqf.delete(qAccountFlow).where(qAccountFlow.id.eq(accountFlowId).and(qAccountFlow.merchantId.eq(merchantId)).and(qAccountFlow.accountBookId.eq(accountFlowBookId))).execute();
    }

    public List<AccountFlow> select(Long merchantId, Long accountFlowBookId) {
        return bqf.selectFrom(qAccountFlow).where(qAccountFlow.merchantId.eq(merchantId).and(qAccountFlow.accountBookId.eq(accountFlowBookId))).fetch();
    }

    public PageResults<AccountFlow> query(Page page, AccountFlowService.Query query) {
        PagedList<AccountFlow> fetchPage = bqf.selectFrom(qAccountFlow).where(query.builder).orderBy(qAccountFlow.id.desc()).fetchPage(page.getOffset(), page.getSize());

        List<AccountFlow> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AccountFlow accountFlow = BeanUtil.toBean(tuple, AccountFlow.class);
            dtos.add(accountFlow);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qAccountFlow.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qAccountFlow.accountBookId, accountBookId);
        }

        public void setStartTime(String startTime) {
            if (StrUtil.isNotEmpty(startTime)) {
                builder.and(qAccountFlow.createdAt.goe(LocalDateTime.parse(startTime + "T00:00:00")));
            }
        }

        public void setEndTime(String endTime) {
            if (StrUtil.isNotEmpty(endTime)) {
                builder.and(qAccountFlow.createdAt.loe(LocalDateTime.parse(endTime + "T23:59:59")));
            }
        }

        public void setAccountId(Long accountId) {
            if (accountId != null) {
                builder.and(qAccountFlow.accountId.eq(accountId));
            }
        }
    }

    public PageResults<OtherFundDetailsVO> queryOtherFundDetails(Page page, OtherFundQuery query) {

        BooleanBuilder condition = new BooleanBuilder();
        Integer type = query.getType();
        if (type == null) {
            throw new ServiceException("type 参数不能为空");
        }

        Long merchantId = query.getMerchantId();
        Long accountBookId = query.getAccountBookId();
        String orderStaffName = query.getOrderStaffName();
        LocalDate startTime = query.getStartTime();
        LocalDate endTime = query.getEndTime();

        if (merchantId != null) {
            if (type == 1) {
                condition.and(QOtherReceipt.otherReceipt.merchantId.eq(merchantId));
            }
            if (type == 2) {
                condition.and(QOtherExpense.otherExpense.merchantId.eq(merchantId));
            }
        }

        if (accountBookId != null) {
            if (type == 1) {
                condition.and(QOtherReceipt.otherReceipt.accountBookId.eq(accountBookId));
            }
            if (type == 2) {
                condition.and(QOtherExpense.otherExpense.accountBookId.eq(accountBookId));
            }
        }

        if (orderStaffName != null && !orderStaffName.isEmpty()) {
            List<BooleanExpression> staffConditions = new ArrayList<>();
            if (type == 1) {
                staffConditions.add(QOtherReceipt.otherReceipt.orderStaffName.like("%" + orderStaffName + "%"));
            }
            if (type == 2) {
                staffConditions.add(QOtherExpense.otherExpense.orderStaffName.like("%" + orderStaffName + "%"));
            }
            if (!staffConditions.isEmpty()) {
                condition.andAnyOf(staffConditions.toArray(new BooleanExpression[0]));
            }
        }

        Long orderStaffId = query.getOrderStaffId();
        if (orderStaffId != null) {
            if (type == 1) {
                condition.and(QOtherReceipt.otherReceipt.orderStaffId.eq(orderStaffId));
            }
            if (type == 2) {
                condition.and(QOtherExpense.otherExpense.orderStaffId.eq(orderStaffId));
            }
        }

        if (startTime != null) {
            if (type == 1) {
                condition.and(QOtherReceipt.otherReceipt.orderDate.goe(startTime));
            }
            if (type == 2) {
                condition.and(QOtherExpense.otherExpense.orderDate.goe(startTime));
            }
        }

        if (endTime != null) {
            if (type == 1) {
                condition.and(QOtherReceipt.otherReceipt.orderDate.loe(endTime));
            }
            if (type == 2) {
                condition.and(QOtherExpense.otherExpense.orderDate.loe(endTime));
            }
        }

        if (type == 1) {
            condition.and(QOtherReceipt.otherReceipt.orderStatus.eq(OrderStatus.已审核));
        }
        if (type == 2) {
            condition.and(QOtherExpense.otherExpense.orderStatus.eq(OrderStatus.已审核));
        }
        JPAQuery<OtherFundDetailsVO> jpaQuery;
        if (type == 1) {
            // 查询收入
            jpaQuery = jqf.select(Projections.bean(OtherFundDetailsVO.class,
                            QOtherReceipt.otherReceipt.orderDate.as("date"),

                            QOtherReceipt.otherReceipt.orderNo.as("documentNumber"),
                            QOtherReceipt.otherReceipt.orderStaffName.as("staffName"),
                            QOtherReceiptItem.otherReceiptItem.accountTypeName.as("accountType"),
                            QOtherReceiptItem.otherReceiptItem.amount.as("amount"),
                            QOtherReceiptItem.otherReceiptItem.remarks,
//                    QOtherReceiptItem.otherReceiptItem.sourceDocNo.as("sourceDocNo"),
                            QOtherReceiptItem.otherReceiptItem.sourceBusinessName.as("businessPartner")
//                    QOtherReceiptItem.otherReceiptItem.sourceDate.as("sourceDate")
                    ))
                    .from(QOtherReceipt.otherReceipt)
                    .join(QOtherReceiptItem.otherReceiptItem).on(QOtherReceiptItem.otherReceiptItem.otherReceiptId.eq(QOtherReceipt.otherReceipt.id))
                    .where(condition);
        } else {
            // 查询支出
            jpaQuery = jqf.select(Projections.bean(OtherFundDetailsVO.class,
//                    QOtherExpense.otherExpense.id,
                            QOtherExpense.otherExpense.orderDate.as("date"),
                            QOtherExpense.otherExpense.orderNo.as("documentNumber"),
                            QOtherExpense.otherExpense.orderStaffName.as("staffName"),
                            QOtherExpenseItem.otherExpenseItem.accountTypeName.as("accountType"),
                            QOtherExpenseItem.otherExpenseItem.amount.as("amount"),
                            QOtherExpenseItem.otherExpenseItem.remarks,
//                    QOtherExpenseItem.otherExpenseItem.sourceDocNo.as("sourceDocNo"),
                            QOtherExpenseItem.otherExpenseItem.sourceBusinessName.as("businessPartner")
//                    QOtherExpenseItem.otherExpenseItem.sourceDate.as("sourceDate")
                    ))
                    .from(QOtherExpense.otherExpense)
                    .join(QOtherExpenseItem.otherExpenseItem).on(QOtherExpenseItem.otherExpenseItem.otherExpenseId.eq(QOtherExpense.otherExpense.id))
                    .where(condition);
        }

        List<OtherFundDetailsVO> list = jpaQuery.offset(page.getOffset())
                .limit(page.getSize())
                .orderBy(type == 1 ? QOtherReceipt.otherReceipt.orderDate.desc() : QOtherExpense.otherExpense.orderDate.desc())
                .fetch();

        long total = jpaQuery.fetchCount();

        return new PageResults<>(list, page, total);
    }

    @Data
    public static class OtherFundQuery implements TenantAware {
        private Long merchantId;
        private Long accountBookId;
        private String orderStaffName;
        private Long orderStaffId;
        private Integer type;
        private LocalDate startTime;
        private LocalDate endTime;
        private OrderStatus status;
    }

}
