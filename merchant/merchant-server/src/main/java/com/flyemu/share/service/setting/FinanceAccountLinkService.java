package com.flyemu.share.service.setting;

import com.flyemu.share.common.TenantAware;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.api.FinOpsCloudApi;
import com.flyemu.share.api.FinOpsRequest;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.VoucherDto;
import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.entity.setting.QFinanceAccountLink;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.repository.setting.FinanceAccountLinkRepository;
import com.flyemu.share.service.BaseService;
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
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceAccountLinkService extends BaseService {

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
            FinOpsRequest finOpsRequest = this.getOpsRequestByLink(financeAccountLink);
            return finOpsCloudApi.loadAccountingCategory(finOpsRequest, null,
                    Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList()));
        }
        return new JSONObject();
    }

    public JSONObject createVoucher(Long accountBookId, VoucherDto dto) throws JsonProcessingException {
        FinanceAccountLink financeAccountLink = this.loadByAccountBookId(accountBookId);
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountBookId);
        dto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
        return finOpsCloudApi.createVoucher(finOpsRequest, null, dto);
    }

    public Integer loadWordCode(Long accountBookId, String word) throws UnsupportedEncodingException {
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountBookId);
        return finOpsCloudApi.loadWordCode(finOpsRequest, null, word, LocalDate.now());
    }

    public Integer loadCode(String word, LocalDate currentAccountDate, AccountDto accountDto) throws UnsupportedEncodingException {
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountDto.getAccountBookId());
        return finOpsCloudApi.loadWordCode(finOpsRequest, null, word, currentAccountDate);
    }

    public Object loadVoucherSelect(AccountDto accountDto) {
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountDto.getAccountBookId());
        return finOpsCloudApi.loadVoucherSelect(finOpsRequest, null);
    }

    public Object loadVoucherSummary(AccountDto accountDto) {
        FinanceAccountLink financeAccountLink = this.loadByAccountBookId(accountDto.getAccountBookId());
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountDto.getAccountBookId());
        return finOpsCloudApi.loadVoucherSummary(finOpsRequest, financeAccountLink.getFinanceAccountId());
    }

    public void upVoucher(FinanceAccountLink financeAccountLink, VoucherDto voucherDto) {
        FinOpsRequest finOpsRequest = this.getOpsRequestByLink(financeAccountLink);
        try {
            finOpsCloudApi.upVoucher(finOpsRequest, null, voucherDto);
        } catch (Exception e) {
            throw new ServiceException("更新凭证失败~");
        }
    }

    private FinOpsRequest getOpsRequestByLink(FinanceAccountLink financeAccountLink) {
        FinOpsRequest finOpsRequest = new FinOpsRequest();
        finOpsRequest.setAccount(financeAccountLink.getFinanceAccount());
        finOpsRequest.setPassword(financeAccountLink.getFinancePassword());
        finOpsRequest.setCallback(cookie -> {
            financeAccountLink.setFinanceCookie(cookie);
            financeAccountLinkRepository.save(financeAccountLink);
        });
        finOpsRequest.setBaseUrl(financeAccountLink.getUrl());
        finOpsRequest.setCookie(finOpsRequest.getCookie());
        return finOpsRequest;
    }

    public Double balance(String subjectId, String categoryId, String categoryDetailsId, AccountDto accountDto) {
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountDto.getAccountBookId());
        return finOpsCloudApi.balance(finOpsRequest, null, subjectId, categoryId, categoryDetailsId);
    }

    public Object loadAuxiliaryAccountingData(List<String> ids, AccountDto accountDto) {
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountDto.getAccountBookId());
        return finOpsCloudApi.loadAuxiliaryAccountingData(finOpsRequest, null, ids);
    }

    private FinOpsRequest getFinOpsRequest(Long accountBookId) {
        FinanceAccountLink financeAccountLink = this.loadByAccountBookId(accountBookId);
        return this.getOpsRequestByLink(financeAccountLink);
    }

    public Object loadVoucher(String voucherId, AccountDto accountDto) {
        FinOpsRequest finOpsRequest = getFinOpsRequest(accountDto.getAccountBookId());
        return finOpsCloudApi.loadVoucher(finOpsRequest, null, voucherId);
    }

    public static class Query implements TenantAware {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qFinanceAccountLink.merchantId, merchantId);
        }

        public void setAccountBookId(Long accountBookId) {
            TenantFilters.accountBook(builder, qFinanceAccountLink.accountBookId, accountBookId);
        }

    }
}
