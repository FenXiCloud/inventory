package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson.JSONArray;
import com.flyemu.share.api.FinOpsCloudApi;
import com.flyemu.share.api.FinOpsRequest;
import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.entity.setting.QFinanceAccountLink;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.FinanceAccountLinkRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * @功能描述: 关联云财务
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceAccountLinkService extends AbsService {

    private final static QFinanceAccountLink qFinanceAccountLink = QFinanceAccountLink.financeAccountLink;

    private final FinanceAccountLinkRepository financeAccountLinkRepository;

    private final FinOpsCloudApi finOpsCloudApi;

    public List<FinanceAccountLink> query(Query query) {
        List<FinanceAccountLink> financeAccountLinks = bqf.selectFrom(qFinanceAccountLink)
                .where(query.builder)
                .orderBy(qFinanceAccountLink.id.desc())
                .fetch();
        return financeAccountLinks;
    }


    @Transactional
    public FinanceAccountLink save(FinanceAccountLink financeAccountLink) {
        if (financeAccountLink.getId() != null) {
            //更新
            FinanceAccountLink original = financeAccountLinkRepository.getById(financeAccountLink.getId());
            BeanUtil.copyProperties(financeAccountLink, original, CopyOptions.create().ignoreNullValue());
            return financeAccountLinkRepository.save(original);
        }
        Long accountBookId = financeAccountLink.getAccountBookId();
        List<FinanceAccountLink> byAccountBookId = financeAccountLinkRepository.findByAccountBookId(accountBookId);
        if (byAccountBookId != null && !byAccountBookId.isEmpty()) {
            throw new ServiceException("账套已关联其他云财务软件账套～");
        }
        return financeAccountLinkRepository.save(financeAccountLink);
    }

    public JSONArray loadAccountSetsList(FinanceAccountLink financeAccountLink, Long merchantId) {
        FinOpsRequest finOpsRequest = new FinOpsRequest();
        finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
        finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
        finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
        return finOpsCloudApi.loadAccountSets(finOpsRequest);
    }

    public FinanceAccountLink loadByAccountBookId(Long accountBookId, Long merchantId) {
        List<FinanceAccountLink> byAccountBookId = financeAccountLinkRepository.findByAccountBookId(accountBookId);
        if (byAccountBookId != null && !byAccountBookId.isEmpty()) {
            return byAccountBookId.get(0);
        }
        return null;
    }

    public FinanceAccountLink load(Long id, Long merchantId) {
        return jqf.selectFrom(qFinanceAccountLink).where(qFinanceAccountLink.id.eq(id))
                .where(qFinanceAccountLink.merchantId.eq(merchantId))
                .fetchOne();
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qFinanceAccountLink.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qFinanceAccountLink.accountBookId.eq(accountBookId));
            }
        }

    }
}
