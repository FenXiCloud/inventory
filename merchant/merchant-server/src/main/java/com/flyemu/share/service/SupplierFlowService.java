package com.flyemu.share.service;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.flyemu.share.entity.basic.SupplierFlow;
import com.flyemu.share.entity.basic.QSupplierFlow;
import com.flyemu.share.repository.SupplierFlowRepository;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @功能描述: 供货商交易流水
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SupplierFlowService extends AbsService {

    private final static QSupplierFlow qSupplierFlow = QSupplierFlow.supplierFlow;

    private final SupplierFlowRepository supplierFlowRepository;

    public List<SupplierFlow> query(Query query) {
        List<SupplierFlow> supplierFlows = bqf.selectFrom(qSupplierFlow)
                .where(query.builder)
                .orderBy(qSupplierFlow.id.desc())
                .fetch();
        return supplierFlows;
    }

    @Transactional
    public SupplierFlow save(SupplierFlow supplierFlow) {
        if (supplierFlow.getId() != null) {
            //更新
            SupplierFlow original = supplierFlowRepository.getById(supplierFlow.getId());
            BeanUtil.copyProperties(supplierFlow, original, CopyOptions.create().ignoreNullValue());
            return supplierFlowRepository.save(original);
        }
        return supplierFlowRepository.save(supplierFlow);
    }

    @Transactional
    public void delete(Long supplierFlowId, Long merchantId, Long accountBookId) {
        jqf.delete(qSupplierFlow)
                .where(qSupplierFlow.id.eq(supplierFlowId).and(qSupplierFlow.merchantId.eq(merchantId)).and(qSupplierFlow.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<SupplierFlow> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qSupplierFlow).where(qSupplierFlow.merchantId.eq(merchantId).and(qSupplierFlow.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qSupplierFlow.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qSupplierFlow.accountBookId.eq(accountBookId));
            }
        }


    }
}
