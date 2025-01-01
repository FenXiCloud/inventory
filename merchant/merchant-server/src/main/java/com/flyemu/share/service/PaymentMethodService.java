package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.entity.basic.QPaymentMethod;
import com.flyemu.share.repository.PaymentMethodRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 结算方式
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PaymentMethodService extends AbsService {

    private final static QPaymentMethod qPaymentMethod = QPaymentMethod.paymentMethod;

    private final PaymentMethodRepository paymentMethodRepository;

    public List<PaymentMethod> query(Query query) {
        List<PaymentMethod> paymentMethods = bqf.selectFrom(qPaymentMethod)
                .where(query.builder)
                .orderBy(qPaymentMethod.id.desc())
                .fetch();
        return paymentMethods;
    }

    @Transactional
    public PaymentMethod save(PaymentMethod paymentMethod) {
        if (paymentMethod.getId() != null) {
            //更新
            PaymentMethod original = paymentMethodRepository.getById(paymentMethod.getId());
            BeanUtil.copyProperties(paymentMethod, original, CopyOptions.create().ignoreNullValue());
            return paymentMethodRepository.save(original);
        }
        return paymentMethodRepository.save(paymentMethod);
    }

    @Transactional
    public void delete(Long paymentMethodId, Long merchantId, Long accountBookId) {
        jqf.delete(qPaymentMethod)
                .where(qPaymentMethod.id.eq(paymentMethodId).and(qPaymentMethod.merchantId.eq(merchantId)).and(qPaymentMethod.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<PaymentMethod> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qPaymentMethod).where(qPaymentMethod.merchantId.eq(merchantId).and(qPaymentMethod.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qPaymentMethod.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qPaymentMethod.accountBookId.eq(accountBookId));
            }
        }
    }
}
