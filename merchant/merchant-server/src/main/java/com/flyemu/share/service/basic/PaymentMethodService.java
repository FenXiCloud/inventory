package com.flyemu.share.service.basic;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.entity.basic.QPaymentMethod;
import com.flyemu.share.entity.fund.QOrderPaymentCollection;
import com.flyemu.share.entity.fund.QOrderReceiptCollection;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.basic.PaymentMethodRepository;
import com.flyemu.share.service.BaseService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentMethodService extends BaseService {

    private final static QPaymentMethod qPaymentMethod = QPaymentMethod.paymentMethod;

    private final PaymentMethodRepository paymentMethodRepository;

    public List<PaymentMethod> query(Query query) {
        return bqf.selectFrom(qPaymentMethod)
                .where(query.builder)
                .orderBy(qPaymentMethod.id.desc())
                .fetch();
    }

    @Transactional
    public PaymentMethod save(PaymentMethod paymentMethod) {
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(qPaymentMethod.merchantId.eq(paymentMethod.getMerchantId()))
                .and(qPaymentMethod.accountBookId.eq(paymentMethod.getAccountBookId()))
                .and(qPaymentMethod.name.eq(paymentMethod.getName()));

        if (paymentMethod.getId() != null) {
            builder.and(qPaymentMethod.id.ne(paymentMethod.getId()));
        }

        Long count = jqf.select(qPaymentMethod.id.count())
                .from(qPaymentMethod)
                .where(builder)
                .fetchOne();

        if (count != null && count > 0) {
            throw new ServiceException("已存在同名结算方式：" + paymentMethod.getName());
        }

        if (paymentMethod.getId() != null) {
            PaymentMethod original = paymentMethodRepository.getById(paymentMethod.getId());
            BeanUtil.copyProperties(paymentMethod, original, CopyOptions.create().ignoreNullValue());
            return paymentMethodRepository.save(original);
        }

        return paymentMethodRepository.save(paymentMethod);
    }

    @Transactional
    public void delete(Long paymentMethodId, Long merchantId, Long accountBookId) {
        BooleanBuilder receiptCondition = new BooleanBuilder();
        receiptCondition.and(QOrderReceiptCollection.orderReceiptCollection.paymentMethodId.eq(Math.toIntExact(paymentMethodId)));
        Long receiptCount = jqf.select(QOrderReceiptCollection.orderReceiptCollection.id.count())
                .from(QOrderReceiptCollection.orderReceiptCollection)
                .where(receiptCondition)
                .fetchOne();

        if (receiptCount != null && receiptCount > 0) {
            throw new ServiceException("该结算方式已被收款单使用，无法删除");
        }

        BooleanBuilder paymentCondition = new BooleanBuilder();
        paymentCondition.and(QOrderPaymentCollection.orderPaymentCollection.paymentMethodId.eq(Math.toIntExact(paymentMethodId)));
        Long paymentCount = jqf.select(QOrderPaymentCollection.orderPaymentCollection.id.count())
                .from(QOrderPaymentCollection.orderPaymentCollection)
                .where(paymentCondition)
                .fetchOne();
        if (paymentCount != null && paymentCount > 0) {
            throw new ServiceException("该结算方式已被付款单使用，无法删除");
        }
        jqf.delete(qPaymentMethod)
                .where(qPaymentMethod.id.eq(paymentMethodId)
                        .and(qPaymentMethod.merchantId.eq(merchantId))
                        .and(qPaymentMethod.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PaymentMethod> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPaymentMethod).where(qPaymentMethod.merchantId.eq(merchantId).and(qPaymentMethod.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (name != null && name != "") {
                builder.and(qPaymentMethod.name.like("%" + name + "%"));
            }
        }

        public void setEnabled(Boolean enabled) {
            if (enabled != null) {
                builder.and(qPaymentMethod.enabled.eq(enabled));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qPaymentMethod.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qPaymentMethod.accountBookId, accountBookId);
        }
    }
}
