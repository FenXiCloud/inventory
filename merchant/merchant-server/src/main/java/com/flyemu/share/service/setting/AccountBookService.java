package com.flyemu.share.service.setting;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.blazebit.persistence.PagedList;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.AccountBookDto;
import com.flyemu.share.entity.setting.*;
import com.flyemu.share.repository.AccountBookRepository;
import com.flyemu.share.service.AbsService;
import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AccountBookService extends AbsService {

    private static final QAccountBook qAccountBook = QAccountBook.accountBook;

    private static final QCheckout qCheckout = QCheckout.checkout;

    private final AccountBookRepository accountBookRepository;
    private final CodeRuleService codeRuleService;

    private final QMerchant qMerchant = QMerchant.merchant;

    public PageResults<AccountBookDto> query(Page page, Query query) {
        PagedList<AccountBook> fetchPage = bqf.selectFrom(qAccountBook).where(query.builder).orderBy(qAccountBook.id.desc()).fetchPage(page.getOffset(), page.getOffsetEnd());

        List<AccountBookDto> dtos = new ArrayList<>();
        fetchPage.forEach(tuple -> {
            AccountBook accountBook = tuple;
            AccountBookDto accountBookDto = BeanUtil.toBean(accountBook, AccountBookDto.class);
            dtos.add(accountBookDto);
        });

        return new PageResults<>(dtos, page, fetchPage.getTotalSize());
    }


    /**
     * loadAccountBooks 加载账套列表
     *
     * @param merchantId
     * @return
     */
    public List<Dict> loadAccountBooks(Long merchantId) {
        List<Dict> dictList = new ArrayList<>();
        bqf.selectFrom(qAccountBook).select(qAccountBook.id, qAccountBook.name, qAccountBook.current, qAccountBook.startDate, qAccountBook.checkoutDate).where(qAccountBook.merchantId.eq(merchantId))
                .orderBy(qAccountBook.id.desc()).fetch().forEach(tuple -> {
                    Dict dict = new Dict().set("key", tuple.get(qAccountBook.id))
                            .set("title", tuple.get(qAccountBook.name))
                            .set("startDate", tuple.get(qAccountBook.startDate))
                            .set("checkoutDate", tuple.get(qAccountBook.checkoutDate))
                            .set("current", tuple.get(qAccountBook.current));
                    dictList.add(dict);
                });
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
            BeanUtil.copyProperties(accountBookDto, original, CopyOptions.create().ignoreNullValue());
            return accountBookRepository.save(original);
        }

        AccountBook accountBook = BeanUtil.toBean(accountBookDto, AccountBook.class);
        accountBookRepository.save(accountBook);
        initializeDefaultCodeRules(accountBook.getMerchantId(), accountBook.getId());
        return accountBook;
    }

    /**
     * 初始化默认编码规则
     */
    private void initializeDefaultCodeRules(Long merchantId, Long accountBookId) {
        List<CodeRule.DocumentType> documentTypes = List.of(
                CodeRule.DocumentType.采购订单,
                CodeRule.DocumentType.采购入库单,
                CodeRule.DocumentType.采购退货单,
                CodeRule.DocumentType.销售订单,
                CodeRule.DocumentType.销售出库单,
                CodeRule.DocumentType.销售退货单,
                CodeRule.DocumentType.调拨单,
                CodeRule.DocumentType.盘点单,
                CodeRule.DocumentType.其他入库单,
                CodeRule.DocumentType.其他出库单,
                CodeRule.DocumentType.成本调整单,
                CodeRule.DocumentType.收款单,
                CodeRule.DocumentType.付款单,
                CodeRule.DocumentType.核销单,
                CodeRule.DocumentType.其他收款单,
                CodeRule.DocumentType.其他付款单,
                CodeRule.DocumentType.转帐单,
                CodeRule.DocumentType.商品,
                CodeRule.DocumentType.仓库,
                CodeRule.DocumentType.客户,
                CodeRule.DocumentType.供货商
        );
        for (CodeRule.DocumentType type : documentTypes) {
            CodeRule rule = new CodeRule();
            rule.setName("初始化");
            rule.setDocumentType(type);
            rule.setPrefix(getDefaultPrefix(type));
            rule.setFormat("yyyyMMdd");
            rule.setSerialNumberLength(5);
            rule.setStartValue(1);
            rule.setResetPeriod(CodeRule.ResetPeriod.日);
            rule.setSystemDefault(true);
            rule.setMerchantId(merchantId);
            rule.setAccountBookId(accountBookId);
            codeRuleService.save(rule);
        }
    }

    /**
     * 获取每个单据类型的默认前缀
     */
    private String getDefaultPrefix(CodeRule.DocumentType type) {
        switch (type) {
            case 采购订单:
                return "PO";
            case 采购入库单:
                return "PI";
            case 采购退货单:
                return "PR";
            case 销售订单:
                return "SO";
            case 销售出库单:
                return "DO";
            case 销售退货单:
                return "SR";
            case 调拨单:
                return "TR";
            case 盘点单:
                return "IC";
            case 其他入库单:
                return "OI";
            case 其他出库单:
                return "OO";
            case 成本调整单:
                return "CA";
            case 收款单:
                return "RC";
            case 付款单:
                return "PY";
            case 核销单:
                return "RV";
            case 其他收款单:
                return "OR";
            case 其他付款单:
                return "OP";
            case 转帐单:
                return "TF";
            case 商品:
                return "PD";
            case 仓库:
                return "WH";
            case 客户:
                return "CU";
            case 供货商:
                return "SU";
            default:
                return "";
        }
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
            if (merchantId != null) {
                builder.and(qAccountBook.merchantId.eq(merchantId));
            }
        }
    }
}
