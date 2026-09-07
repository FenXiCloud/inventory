package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.common.TenantFilters;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AccountBookDto;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.setting.AccountBookRepository;
import com.flyemu.share.service.BaseService;
import com.flyemu.share.service.basic.PriceResolveService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountBookService extends BaseService {

    private static final QAccountBook qAccountBook = QAccountBook.accountBook;

    private static final QCheckout qCheckout = QCheckout.checkout;

    private static final QMonthlyCloseLog qMonthlyCloseLog = QMonthlyCloseLog.monthlyCloseLog;

    private static final QMonthlyInventorySummary qMonthlyInventorySummary = QMonthlyInventorySummary.monthlyInventorySummary;

    private static final QAccountBookParameters qAccountBookParameters = QAccountBookParameters.accountBookParameters;

    private final AccountBookRepository accountBookRepository;
    private final CodeRuleService codeRuleService;
    private final PriceResolveService priceResolveService;

    private final QMerchant qMerchant = QMerchant.merchant;

    public PageResults<AccountBookDto> query(Page page, Query query) {
        PagedList<AccountBook> fetchPage = bqf.selectFrom(qAccountBook).where(query.builder).orderBy(qAccountBook.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());
        List<AccountBook> accountBooks = new ArrayList<>();
        fetchPage.forEach(accountBooks::add);
        Map<Integer, AccountBookParameters> paramsMap = loadParameters(accountBooks);
        List<AccountBookDto> dtos = new ArrayList<>();
        for (AccountBook accountBook : accountBooks) {
            AccountBookDto dto = BeanUtil.toBean(accountBook, AccountBookDto.class);
            AccountBookParameters parameters = paramsMap.get(accountBook.getId().intValue());
            if (parameters != null) {
                dto.setCostAccounting(parameters.getCostAccounting());
                dto.setAvailableInventory(parameters.getAvailableInventory());
                dto.setQuantityDecimal(parameters.getQuantityDecimal());
                dto.setPriceDecimal(parameters.getPriceDecimal());
            }
            dtos.add(dto);
        }
        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }

    /**
     * 按本页账套批量加载参数设置，避免逐行查询
     */
    private Map<Integer, AccountBookParameters> loadParameters(List<AccountBook> accountBooks) {
        if (accountBooks.isEmpty()) {
            return Map.of();
        }
        List<Integer> ids = accountBooks.stream().map(ab -> ab.getId().intValue()).toList();
        return bqf.selectFrom(qAccountBookParameters)
                .where(qAccountBookParameters.accountBookId.in(ids))
                .fetch().stream()
                .collect(Collectors.toMap(AccountBookParameters::getAccountBookId, Function.identity(), (a, b) -> a));
    }

    /**
     * loadAccountBooks 加载账套列表（附全局生效的账套参数：小数位/成本法/负库存开关，供前端 store 全局消费）
     *
     * @param merchantId
     * @return
     */
    public List<Dict> loadAccountBooks(Long merchantId) {
        List<AccountBook> accountBooks = bqf.selectFrom(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId))
                .orderBy(qAccountBook.id.desc()).fetch();
        Map<Integer, AccountBookParameters> paramsMap = loadParameters(accountBooks);
        List<Dict> dictList = new ArrayList<>();
        for (AccountBook accountBook : accountBooks) {
            AccountBookParameters parameters = paramsMap.get(accountBook.getId().intValue());
            Dict dict = new Dict().set("key", accountBook.getId())
                    .set("title", accountBook.getName())
                    .set("startDate", accountBook.getStartDate())
                    .set("checkoutDate", accountBook.getCheckoutDate())
                    .set("current", accountBook.getCurrent())
                    // 缺省与 AccountBookParametersService.findOrCreate 保持一致：2/2/移动平均/否
                    .set("quantityDecimal", parameters != null && parameters.getQuantityDecimal() != null ? parameters.getQuantityDecimal() : 2)
                    .set("priceDecimal", parameters != null && parameters.getPriceDecimal() != null ? parameters.getPriceDecimal() : 2)
                    .set("costAccounting", parameters != null && parameters.getCostAccounting() != null ? parameters.getCostAccounting() : 1)
                    .set("availableInventory", parameters != null && parameters.getAvailableInventory() != null ? parameters.getAvailableInventory() : 2);
            dictList.add(dict);
        }
        return dictList;
    }

    @Transactional
    public AccountBook changeCurrentAccountBook(Long merchantId, Long orgId) {
        jqf.update(qAccountBook)
                .set(qAccountBook.current, false)
                .where(qAccountBook.merchantId.eq(merchantId)).execute();
        jqf.update(qAccountBook)
                .set(qAccountBook.current, true)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(orgId))).execute();

        return bqf.selectFrom(qAccountBook).where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(orgId))).fetchFirst();
    }

    /**
     * 保存/更新
     *
     * @param accountBookDto
     * @return
     */
    @Transactional
    public AccountBook save(AccountBookDto accountBookDto) {
        if (bqf.selectFrom(qAccountBook)
                .where(qAccountBook.merchantId.eq(accountBookDto.getMerchantId())).fetchCount() <= 0) {
            accountBookDto.setCurrent(true);
        }
        if (accountBookDto.getId() != null) {
            //更新
            AccountBook original = accountBookRepository.getById(accountBookDto.getId());
            // 如果修改了启用日期，清空结账日期，以便从新启用月份重新开始结账
            if (accountBookDto.getStartDate() != null && !accountBookDto.getStartDate().equals(original.getStartDate())) {
                original.setCheckoutDate(null);
                Long merchantId = original.getMerchantId();
                Long accountBookId = original.getId();
                // 删除已有的结账记录
                jqf.delete(qCheckout)
                        .where(qCheckout.merchantId.eq(merchantId)
                                .and(qCheckout.accountBookId.eq(accountBookId)))
                        .execute();
                // 删除结账日志
                jqf.delete(qMonthlyCloseLog)
                        .where(qMonthlyCloseLog.merchantId.eq(merchantId)
                                .and(qMonthlyCloseLog.accountBookId.eq(accountBookId)))
                        .execute();
                // 删除月结库存表
                jqf.delete(qMonthlyInventorySummary)
                        .where(qMonthlyInventorySummary.merchantId.eq(merchantId)
                                .and(qMonthlyInventorySummary.accountBookId.eq(accountBookId)))
                        .execute();
                log.info("启用日期变更，已清除账套[{}]的所有结账数据", accountBookId);
            }
            BeanUtil.copyProperties(accountBookDto, original, CopyOptions.create().ignoreNullValue());
            return accountBookRepository.save(original);
        }

        AccountBook accountBook = BeanUtil.toBean(accountBookDto, AccountBook.class);
        accountBookRepository.save(accountBook);
        codeRuleService.ensureDefaultRules(accountBook.getMerchantId(), accountBook.getId());
        priceResolveService.ensureDefaultPolicies(accountBook.getMerchantId(), accountBook.getId());
        return accountBook;
    }

    /**
     * 删除
     *
     * @param merchantId
     * @param accountBookId
     */
    @Transactional
    public void delete(Long merchantId, Long accountBookId) {

        jqf.delete(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .execute();
    }

    /**
     * 根据ID获取数据
     *
     * @param accountBookId
     * @return
     */
    public AccountBook loadById(Long merchantId, Long accountBookId) {
        return bqf.selectFrom(qAccountBook)
                .where(qAccountBook.merchantId.eq(merchantId).and(qAccountBook.id.eq(accountBookId)))
                .fetchFirst();
    }

    public List<AccountBook> select(Long merchantId) {
        return bqf.selectFrom(qAccountBook).where(qAccountBook.merchantId.eq(merchantId)).fetch();
    }

    /**
     * 查询条件
     */
    public static class Query {
        public final BooleanBuilder builder = new BooleanBuilder();

        public void setName(String name) {
            if (StrUtil.isNotEmpty(name)) {
                builder.and(qAccountBook.name.contains(name));
            }
        }

        public void setMerchantId(Long merchantId) {
            TenantFilters.merchant(builder, qAccountBook.merchantId, merchantId);
        }
    }
}
