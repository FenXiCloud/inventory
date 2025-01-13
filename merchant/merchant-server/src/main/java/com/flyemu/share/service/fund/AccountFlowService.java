package com.flyemu.share.service.fund;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.fund.AccountFlow;
import com.flyemu.share.entity.fund.QAccountFlow;
import com.flyemu.share.repository.AccountFlowRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 账户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountFlowService extends AbsService {

    private final static QAccountFlow qAccountFlow = QAccountFlow.accountFlow;

    private final AccountFlowRepository accountFlowRepository;

    public List<AccountFlow> query(Query query) {
        List<AccountFlow> accountFlows = bqf.selectFrom(qAccountFlow)
                .where(query.builder)
                .orderBy(qAccountFlow.id.desc())
                .fetch();
        return accountFlows;
    }

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
        jqf.delete(qAccountFlow)
                .where(qAccountFlow.id.eq(accountFlowId).and(qAccountFlow.merchantId.eq(merchantId)).and(qAccountFlow.accountBookId.eq(accountFlowBookId)))
                .execute();
    }

    public List<AccountFlow> select(Long merchantId, Long accountFlowBookId) {
        return bqf.selectFrom(qAccountFlow).where(qAccountFlow.merchantId.eq(merchantId).and(qAccountFlow.accountBookId.eq(accountFlowBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qAccountFlow.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qAccountFlow.accountBookId.eq(accountBookId));
            }
        }
    }
}
