package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.entity.inventory.QOtherOutbound;
import com.flyemu.share.repository.OtherOutboundRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * @功能描述: 其他入库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OtherOutboundService extends AbsService {

    private final static QOtherOutbound qOtherOutbound = QOtherOutbound.otherOutbound;

    private final OtherOutboundRepository otherOutboundRepository;

    public PageResults<OtherOutbound> query(Page page, Query query) {
        PagedList<OtherOutbound> fetchPage = bqf.selectFrom(qOtherOutbound).where(query.builder).orderBy(qOtherOutbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherOutbound> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherOutbound otherOutbound1 = tuple;
            OtherOutbound otherOutbound = BeanUtil.toBean(otherOutbound1, OtherOutbound.class);
            dtos.add(otherOutbound);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherOutbound save(OtherOutbound otherOutbound) {
        if (otherOutbound.getId() != null) {
            //更新
            OtherOutbound original = otherOutboundRepository.getById(otherOutbound.getId());
            BeanUtil.copyProperties(otherOutbound, original, CopyOptions.create().ignoreNullValue());
            return otherOutboundRepository.save(original);
        }
        return otherOutboundRepository.save(otherOutbound);
    }

    @Transactional
    public void delete(Long otherOutboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherOutbound)
                .where(qOtherOutbound.id.eq(otherOutboundId).and(qOtherOutbound.merchantId.eq(merchantId)).and(qOtherOutbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherOutbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherOutbound).where(qOtherOutbound.merchantId.eq(merchantId).and(qOtherOutbound.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherOutbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherOutbound.accountBookId.eq(accountBookId));
            }
        }
    }
}
