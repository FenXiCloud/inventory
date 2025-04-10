package com.flyemu.share.service.setting;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.flyemu.share.dto.AcDetailsDto;
import com.flyemu.share.dto.VoucherDetailsDto;
import com.flyemu.share.dto.VoucherDto;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.FinanceVoucherForm;
import com.flyemu.share.repository.FinanceVoucherRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


/**
 * @功能描述: 云财务凭证
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FinanceVoucherService extends AbsService {

    private final static QFinanceVoucher qFinanceVoucher = QFinanceVoucher.financeVoucher;

    private final FinanceVoucherRepository financeVoucherRepository;

    private final FinanceAccountLinkService financeAccountLinkService;

    private final FinanceVoucherTemplateService financeVoucherTemplateService;

    private final FinanceItemMapService financeItemMapService;


    public List<FinanceVoucher> query(FinanceVoucherService.Query query) {
        return bqf.selectFrom(qFinanceVoucher)
                .where(query.builder)
                .orderBy(qFinanceVoucher.id.desc())
                .fetch();
    }

    @Transactional
    public void save(FinanceVoucherForm financeVoucherForm) throws JsonProcessingException, UnsupportedEncodingException {
        String type = financeVoucherForm.getType();
        BigDecimal amount = financeVoucherForm.getAmount();
        Long orderId = financeVoucherForm.getOrderId();
        Long accountBookId = financeVoucherForm.getAccountBookId();
        FinanceAccountLink financeAccountLink = financeAccountLinkService.loadByAccountBookId(accountBookId);
        if (financeAccountLink == null) {
            throw new ServiceException("未配置关联财务云软件～");
        }
        FinanceVoucherTemplate financeVoucherTemplate = financeVoucherTemplateService.findByType(type);
        if (financeVoucherTemplate == null) {
            throw new ServiceException("未配置对应凭证模板～");
        }
        VoucherDto voucherDto = new VoucherDto();
        voucherDto.setCreditAmount(amount);
        voucherDto.setDebitAmount(amount);
        voucherDto.setVoucherDate(financeVoucherForm.getOrderTime());
        voucherDto.setCreateDate(LocalDateTime.now());
        voucherDto.setWord(financeVoucherTemplate.getWord());
        voucherDto.setRemark(financeVoucherForm.getRemark());
        List<VoucherDetailsDto> detailsDtos = new ArrayList<>();
        JSONArray details = financeVoucherTemplate.getDetails();
        VoucherDetailsDto voucherDetailsDto;
        for (int i = 0; i < details.size(); i++) {
            voucherDetailsDto = new VoucherDetailsDto();
            voucherDetailsDto.setSummary(financeVoucherForm.getOrderName());
            JSONObject jsonObject = details.getJSONObject(i);
            JSONArray auxiliaryAccounting = jsonObject.getJSONArray("auxiliaryAccounting");
            if (auxiliaryAccounting == null) {
                auxiliaryAccounting = new JSONArray();
            }
            List<AcDetailsDto> auxiliaryAccountingList = new ArrayList<>();
            AcDetailsDto acDetailsDto;
            for (int j = 0; j < auxiliaryAccounting.size(); j++) {
                String categoryType = auxiliaryAccounting.getString(0);
                FinanceItemMap financeItemMap = financeItemMapService.findByCategoryType(categoryType);
                if (financeItemMap == null) {
                    throw new ServiceException("未配置辅助映射～");
                }
                Long inventoryId = financeItemMap.getInventoryId();
                acDetailsDto = new AcDetailsDto();
                switch (categoryType) {
                    case "0":
                        // 客户
                        if (inventoryId.equals(financeVoucherForm.getCustomerId())) {
                            acDetailsDto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
                            acDetailsDto.setCategoryId(financeItemMap.getCategoryId());
                            acDetailsDto.setCategoryName(financeItemMap.getCategoryName());
                            acDetailsDto.setCategoryType(financeItemMap.getCategoryType());
                            acDetailsDto.setCreateTime(LocalDateTime.now());
                        }
                        break;
                    case "1":
                        // 供应商
                        if (inventoryId.equals(financeVoucherForm.getSupplierId())) {
                            acDetailsDto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
                            acDetailsDto.setCategoryId(financeItemMap.getCategoryId());
                            acDetailsDto.setCategoryName(financeItemMap.getCategoryName());
                            acDetailsDto.setCategoryType(financeItemMap.getCategoryType());
                            acDetailsDto.setCreateTime(LocalDateTime.now());
                        }
                        break;
                    case "6":
                        // 商品
                        if (inventoryId.equals(financeVoucherForm.getProductId())) {
                            acDetailsDto.setAccountSetsId(financeAccountLink.getFinanceAccountId());
                            acDetailsDto.setCategoryId(financeItemMap.getCategoryId());
                            acDetailsDto.setCategoryName(financeItemMap.getCategoryName());
                            acDetailsDto.setCategoryType(financeItemMap.getCategoryType());
                            acDetailsDto.setCreateTime(LocalDateTime.now());
                        }
                        break;
                }
                auxiliaryAccountingList.add(acDetailsDto);
            }
            Long subjectId = jsonObject.getLong("subjectId");
            String subjectName = jsonObject.getString("subjectName");
            voucherDetailsDto.setSubjectId(subjectId);
            voucherDetailsDto.setSubjectName(subjectName);
            if ((i + 1) % 2 == 0) {
                voucherDetailsDto.setCreditAmount(amount);
            } else {
                voucherDetailsDto.setDebitAmount(amount);
            }
            voucherDetailsDto.setAuxiliary(auxiliaryAccountingList);
            detailsDtos.add(voucherDetailsDto);
        }
        voucherDto.setDetails(detailsDtos);
        voucherDto.setInsert(true);
        voucherDto.setIsTemplate(false);
        Integer wordCode = financeAccountLinkService.loadWordCode(accountBookId, voucherDto.getWord());
        voucherDto.setCode(wordCode);
        JSONObject jsonObject = financeAccountLinkService.createVoucher(accountBookId, voucherDto);
        log.info(jsonObject.toJSONString());
        String code = jsonObject.getString("code");
        FinanceVoucher financeVoucher = new FinanceVoucher();
        financeVoucher.setCode(code);
        financeVoucher.setAccountBookId(accountBookId);
        financeVoucher.setOrderId(orderId);
        financeVoucher.setParams(JSONObject.toJSONString(voucherDto));
        financeVoucher.setAccountBookId(accountBookId);
        financeVoucher.setCreatedAt(LocalDateTime.now());
        financeVoucher.setMerchantId(financeVoucherForm.getMerchantId());
        financeVoucher.setType(type);
        financeVoucher.setCreatedBy(financeVoucherTemplate.getCreatedBy());
        financeVoucherRepository.save(financeVoucher);
    }

    @Transactional
    public void delete(Long id, Long merchantId, Long accountBookId) {
        jqf.delete(qFinanceVoucher)
                .where(qFinanceVoucher.id.eq(id).and(qFinanceVoucher.merchantId.eq(merchantId)).and(qFinanceVoucher.accountBookId.eq(accountBookId)))
                .execute();
    }

    public FinanceVoucher load(Long id) {
        return financeVoucherRepository.findById(id).orElseThrow(RuntimeException::new);
    }

    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setMerchantId(Long merchantId) {
            if (merchantId != null) {
                builder.and(qFinanceVoucher.merchantId.eq(merchantId));
            }
        }

        public void setAccountBookId(Long accountBookId) {
            if (accountBookId != null) {
                builder.and(qFinanceVoucher.accountBookId.eq(accountBookId));
            }
        }

    }
}
