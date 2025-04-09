package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.flyemu.share.api.FinOpsCloudApi;
import com.flyemu.share.api.FinOpsRequest;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.VoucherDto;
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

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


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
        return bqf.selectFrom(qFinanceAccountLink)
                .where(query.builder)
                .orderBy(qFinanceAccountLink.id.desc())
                .fetch();
    }


    @Transactional
    public FinanceAccountLink save(FinanceAccountLink financeAccountLink, AccountDto accountDto) {
        if (financeAccountLink.getId() != null) {
            //更新
            FinanceAccountLink original = financeAccountLinkRepository.getById(financeAccountLink.getId());
            BeanUtil.copyProperties(financeAccountLink, original, CopyOptions.create().ignoreNullValue());
            original.setUpdatedAt(LocalDateTime.now());
            return financeAccountLinkRepository.save(original);
        }
        Long accountBookId = financeAccountLink.getAccountBookId();
        List<FinanceAccountLink> byAccountBookId = financeAccountLinkRepository.findByAccountBookId(accountBookId);
        if (byAccountBookId != null && !byAccountBookId.isEmpty()) {
            throw new ServiceException("账套已关联其他云财务软件账套～");
        }
        financeAccountLink.setCreatedAt(LocalDateTime.now());
        financeAccountLink.setCreatedBy(accountDto.getAdminId());
        return financeAccountLinkRepository.save(financeAccountLink);
    }

    public JSONObject loadAccountSetsList(FinanceAccountLink financeAccountLink, Long merchantId) {
        JSONObject result = new JSONObject();
        FinOpsRequest finOpsRequest = new FinOpsRequest();
        finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
        finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
        finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
        String cookie = finOpsCloudApi.getCookie(finOpsRequest);
        finOpsRequest.setCookie(cookie);
        JSONArray array = finOpsCloudApi.loadAccountSets(finOpsRequest);
        result.put("data", array);
        result.put("cookie", cookie);
        return result;
    }

    public FinanceAccountLink loadByAccountBookId(Long accountBookId) {
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

    public JSONArray loadVoucherWord(AccountDto accountDto) {
        Long accountBookId = accountDto.getAccountBookId();
        List<FinanceAccountLink> byAccountBookId = financeAccountLinkRepository.findByAccountBookId(accountBookId);
        if (byAccountBookId != null && !byAccountBookId.isEmpty()) {
            FinanceAccountLink financeAccountLink = byAccountBookId.get(0);
            FinOpsRequest finOpsRequest = new FinOpsRequest();
            finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
            finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
            finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
            finOpsRequest.setCookie(finOpsRequest.getCookie());
            return finOpsCloudApi.loadVoucherWord(finOpsRequest, financeAccountLink.getFinanceAccountId());
        }
        return new JSONArray();
    }

    public JSONArray loadSubject(AccountDto accountDto) {
        Long accountBookId = accountDto.getAccountBookId();
        List<FinanceAccountLink> byAccountBookId = financeAccountLinkRepository.findByAccountBookId(accountBookId);
        if (byAccountBookId != null && !byAccountBookId.isEmpty()) {
            FinanceAccountLink financeAccountLink = byAccountBookId.get(0);
            FinOpsRequest finOpsRequest = new FinOpsRequest();
            finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
            finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
            finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
            finOpsRequest.setCookie(finOpsRequest.getCookie());
            return finOpsCloudApi.loadSubject(finOpsRequest, financeAccountLink.getFinanceAccountId());
        }
        return new JSONArray();
    }

    @Transactional
    public JSONObject loadAccountingCategory(String ids, AccountDto accountDto) {
        Long accountBookId = accountDto.getAccountBookId();
        List<FinanceAccountLink> byAccountBookId = financeAccountLinkRepository.findByAccountBookId(accountBookId);
        if (byAccountBookId != null && !byAccountBookId.isEmpty()) {
            FinanceAccountLink financeAccountLink = byAccountBookId.get(0);
            FinOpsRequest finOpsRequest = new FinOpsRequest();
            finOpsRequest.setCallback(cookie -> {
                financeAccountLink.setFinanceCookie(cookie);
                financeAccountLinkRepository.save(financeAccountLink);
            });
            finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
            finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
            finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
            finOpsRequest.setCookie(finOpsRequest.getCookie());
            return finOpsCloudApi.loadAccountingCategory(finOpsRequest, null,
                    Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList()));
        }
        return new JSONObject();
    }

    public JSONObject createVoucher(Long accountBookId, VoucherDto dto) throws JsonProcessingException {
        FinanceAccountLink financeAccountLink = this.loadByAccountBookId(accountBookId);
        FinOpsRequest finOpsRequest = new FinOpsRequest();
        finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
        finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
        finOpsRequest.setCallback(cookie -> {
            financeAccountLink.setFinanceCookie(cookie);
            financeAccountLinkRepository.save(financeAccountLink);
        });
        finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
        finOpsRequest.setCookie(finOpsRequest.getCookie());
        dto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
        return finOpsCloudApi.createVoucher(finOpsRequest, null, dto);
    }

    public Integer loadWordCode(Long accountBookId, String word) throws UnsupportedEncodingException {
        FinanceAccountLink financeAccountLink = this.loadByAccountBookId(accountBookId);
        FinOpsRequest finOpsRequest = new FinOpsRequest();
        finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
        finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
        finOpsRequest.setCallback(cookie -> {
            financeAccountLink.setFinanceCookie(cookie);
            financeAccountLinkRepository.save(financeAccountLink);
        });
        finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
        finOpsRequest.setCookie(finOpsRequest.getCookie());
        return finOpsCloudApi.loadWordCode(finOpsRequest, null, word, LocalDate.now());
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
