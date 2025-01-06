package com.flyemu.share.service.inventory;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.entity.inventory.QOtherInbound;
import com.flyemu.share.repository.OtherInboundRepository;
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
public class OtherInboundService extends AbsService {

    private final static QOtherInbound qOtherInbound = QOtherInbound.otherInbound;

    private final OtherInboundRepository otherInboundRepository;

    public PageResults<OtherInbound> query(Page page, Query query) {
        PagedList<OtherInbound> fetchPage = bqf.selectFrom(qOtherInbound).where(query.builder).orderBy(qOtherInbound.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<OtherInbound> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            OtherInbound otherInbound1 = tuple;
            OtherInbound otherInbound = BeanUtil.toBean(otherInbound1, OtherInbound.class);
            dtos.add(otherInbound);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    @Transactional
    public OtherInbound save(OtherInbound otherInbound) {
        if (otherInbound.getId() != null) {
            //更新
            OtherInbound original = otherInboundRepository.getById(otherInbound.getId());
            BeanUtil.copyProperties(otherInbound, original, CopyOptions.create().ignoreNullValue());
            return otherInboundRepository.save(original);
        }
        return otherInboundRepository.save(otherInbound);
    }

    @Transactional
    public void delete(Long otherInboundId, Long merchantId, Long accountBookId) {
        jqf.delete(qOtherInbound)
                .where(qOtherInbound.id.eq(otherInboundId).and(qOtherInbound.merchantId.eq(merchantId)).and(qOtherInbound.accountBookId.eq(accountBookId)))
                .execute();
    }

    public List<OtherInbound> select(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qOtherInbound).where(qOtherInbound.merchantId.eq(merchantId).and(qOtherInbound.accountBookId.eq(accountBookId))).fetch();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qOtherInbound.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qOtherInbound.accountBookId.eq(accountBookId));
            }
        }
    }
}
