package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.CustomerFlow;
import com.flyemu.share.entity.basic.QCustomerFlow;
import com.flyemu.share.repository.CustomerFlowRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 客户交易流水
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CustomerFlowService extends AbsService {

    private final static QCustomerFlow qCustomerFlow = QCustomerFlow.customerFlow;

    private final CustomerFlowRepository customerFlowRepository;

    public List<CustomerFlow> query(Query query) {
        List<CustomerFlow> customerFlows = bqf.selectFrom(qCustomerFlow)
                .where(query.builder)
                .orderBy(qCustomerFlow.id.desc())
                .fetch();
        return customerFlows;
    }

    @Transactional
    public CustomerFlow save(CustomerFlow customerFlow) {
        if (customerFlow.getId() != null) {
            //更新
            CustomerFlow original = customerFlowRepository.getById(customerFlow.getId());
            BeanUtil.copyProperties(customerFlow, original, CopyOptions.create().ignoreNullValue());
            return customerFlowRepository.save(original);
        }
        return customerFlowRepository.save(customerFlow);
    }

    @Transactional
    public void delete(Long customerFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qCustomerFlow)
                .where(qCustomerFlow.id.eq(customerFlowId).and(qCustomerFlow.merchantId.eq(merchantId)).and(qCustomerFlow.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<CustomerFlow> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCustomerFlow).where(qCustomerFlow.merchantId.eq(merchantId).and(qCustomerFlow.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCustomerFlow.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCustomerFlow.accountBookId.eq(accountBookId));
            }
        }

    }
}
