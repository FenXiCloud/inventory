package com.flyemu.share.service.setting;

import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.entity.setting.AccountBook;
import com.flyemu.share.repository.setting.AccountBookParametersRepository;
import com.flyemu.share.service.BaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/** 账套参数设置 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountBookParametersService extends BaseService {
    private static final QAccountBookParameters Q_ACCOUNT_BOOK_PARAMETERS = QAccountBookParameters.accountBookParameters;

    private final AccountBookParametersRepository accountBookParametersRepository;
    private final AccountBookService accountBookService;

    @Transactional
    public AccountBookParameters load(Long merchantId, Integer accountBookId) {
        Assert.notNull(accountBookId, "id不能为空");
        AccountBook accountBook = accountBookService.loadById(merchantId, accountBookId.longValue());
        Assert.notNull(accountBook, "账套不存在");
        return findOrCreate(accountBookId);
    }

    @Transactional
    public AccountBookParameters list(Integer id) {
        return findOrCreate(id);
    }

    private AccountBookParameters findOrCreate(Integer accountBookId) {
        Assert.notNull(accountBookId, "id不能为空");
        AccountBookParameters params = bqf
                .selectFrom(Q_ACCOUNT_BOOK_PARAMETERS)
                .where(Q_ACCOUNT_BOOK_PARAMETERS.accountBookId.eq(accountBookId))
                .fetchFirst();
        if (params == null) {
            params = new AccountBookParameters();
            params.setAccountBookId(accountBookId);
            params.setCostAccounting(1);
            params.setAvailableInventory(2);
            params.setQuantityDecimal(2);
            params.setPriceDecimal(2);
            params.setCreateTime(new Date());
            params.setUpdateTime(new Date());
            accountBookParametersRepository.save(params);
        }
        return params;
    }

    @Transactional
    public void update(AccountBookParameters accountBookParameters) {
        Assert.notNull(accountBookParameters.getId(), "id不能为空");
        AccountBookParameters existing = accountBookParametersRepository.findById(accountBookParameters.getId())
                .orElseThrow(() -> new IllegalArgumentException("账套参数不存在"));
        Integer originalQuantityDecimal = existing.getQuantityDecimal();
        Integer newPriceDecimal = accountBookParameters.getPriceDecimal();
        Assert.notNull(newPriceDecimal, "单价小数位不能为空");
        Assert.isTrue(newPriceDecimal >= 0 && newPriceDecimal <= 8,
                "单价小数位必须在 0~8 之间");
        Integer newQuantityDecimal = accountBookParameters.getQuantityDecimal();
        Assert.notNull(newQuantityDecimal, "数量小数位不能为空");
        Assert.isTrue(newQuantityDecimal >= 0 && newQuantityDecimal <= 8,
                "数量小数位必须在 0~8 之间");
        if (originalQuantityDecimal != null && newQuantityDecimal < originalQuantityDecimal) {
            throw new IllegalArgumentException("数量小数位不能由大改小（原值：" + originalQuantityDecimal + "）");
        }
        Assert.notNull(accountBookParameters.getCostAccounting(), "成本核算方法不能为空");
        Assert.notNull(accountBookParameters.getAvailableInventory(), "可用库存允许为负不能为空");

        existing.setCostAccounting(accountBookParameters.getCostAccounting());
        existing.setAvailableInventory(accountBookParameters.getAvailableInventory());
        existing.setQuantityDecimal(newQuantityDecimal);
        existing.setPriceDecimal(newPriceDecimal);
        existing.setUpdateTime(new Date());
        accountBookParametersRepository.save(existing);
    }
}
