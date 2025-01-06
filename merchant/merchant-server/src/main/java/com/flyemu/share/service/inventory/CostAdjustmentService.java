package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.CostAdjustment;
import com.flyemu.share.entity.inventory.QCostAdjustment;
import com.flyemu.share.repository.CostAdjustmentRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 成本调整单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CostAdjustmentService extends AbsService {

    private final static QCostAdjustment qCostAdjustment = QCostAdjustment.costAdjustment;

    private final CostAdjustmentRepository costAdjustmentRepository;

    public PageResults<CostAdjustment> query(Page page, Query query) {
        PagedList<CostAdjustment> fetchPage = bqf.selectFrom(qCostAdjustment).where(query.builder).orderBy(qCostAdjustment.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<CostAdjustment> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            CostAdjustment costAdjustment1 = tuple;
            CostAdjustment costAdjustment = BeanUtil.toBean(costAdjustment1, CostAdjustment.class);
            dtos.add(costAdjustment);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public CostAdjustment save(CostAdjustment costAdjustment) {
        if (costAdjustment.getId() != null) {
            //更新
            CostAdjustment original = costAdjustmentRepository.getById(costAdjustment.getId());
            BeanUtil.copyProperties(costAdjustment, original, CopyOptions.create().ignoreNullValue());
            return costAdjustmentRepository.save(original);
        }
        return costAdjustmentRepository.save(costAdjustment);
    }

    @Transactional
    public void delete(Long costAdjustmentId, Long merchantId, Long accountBookId) {
        jqf.delete(qCostAdjustment)
                .where(qCostAdjustment.id.eq(costAdjustmentId).and(qCostAdjustment.merchantId.eq(merchantId)).and(qCostAdjustment.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<CostAdjustment> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qCostAdjustment).where(qCostAdjustment.merchantId.eq(merchantId).and(qCostAdjustment.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qCostAdjustment.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qCostAdjustment.accountBookId.eq(accountBookId));
            }
        }
    }
}
