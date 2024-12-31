package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.SettlementMethod;
import com.flyemu.share.entity.basic.QSettlementMethod;
import com.flyemu.share.entity.basic.QSettlementMethod;
import com.flyemu.share.repository.SettlementMethodRepository;
import com.flyemu.share.repository.SettlementMethodRepository;
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
public class SettlementMethodService extends AbsService {

    private final static QSettlementMethod qSettlementMethod = QSettlementMethod.settlementMethod;

    private final SettlementMethodRepository settlementMethodRepository;

    public List<SettlementMethod> query(Query query) {
        List<SettlementMethod> settlementMethods = bqf.selectFrom(qSettlementMethod)
                .where(query.builder)
                .orderBy(qSettlementMethod.id.desc())
                .fetch();
        return settlementMethods;
    }

    @Transactional
    public SettlementMethod save(SettlementMethod settlementMethod) {
        if (settlementMethod.getId() != null) {
            //更新
            SettlementMethod original = settlementMethodRepository.getById(settlementMethod.getId());
            BeanUtil.copyProperties(settlementMethod, original, CopyOptions.create().ignoreNullValue());
            return settlementMethodRepository.save(original);
        }
        return settlementMethodRepository.save(settlementMethod);
    }

    @Transactional
    public void delete(Long SettlementMethodId, Long merchantId, Long accountBookId) {
        jqf.delete(qSettlementMethod)
                .where(qSettlementMethod.id.eq(SettlementMethodId).and(qSettlementMethod.merchantId.eq(merchantId)).and(qSettlementMethod.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SettlementMethod> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSettlementMethod).where(qSettlementMethod.merchantId.eq(merchantId).and(qSettlementMethod.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSettlementMethod.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSettlementMethod.accountBookId.eq(accountBookId));
            }
        }
    }
}
