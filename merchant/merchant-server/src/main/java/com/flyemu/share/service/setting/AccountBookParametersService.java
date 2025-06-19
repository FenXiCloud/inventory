package com.flyemu.share.service.setting;


import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.repository.AccountBookParametersRepository;
import com.flyemu.share.service.AbsService;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;


/**
 * 账套参数设置业务层
 *
 * @author shuaiqi
 * @since 2025-05-13 15:35:40
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountBookParametersService extends AbsService {
    private static final QAccountBookParameters Q_ACCOUNT_BOOK_PARAMETERS = QAccountBookParameters.accountBookParameters;

    private final AccountBookParametersRepository accountBookParametersRepository;


    public AccountBookParameters list(Integer id) {
        Assert.notNull(id, "id不能为空");
        AccountBookParameters params = bqf
                .selectFrom(Q_ACCOUNT_BOOK_PARAMETERS)
                .where(Q_ACCOUNT_BOOK_PARAMETERS.accountBookId.eq(id))
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .fetchFirst();
        if (params == null) {
            params = new AccountBookParameters();
            params.setAccountBookId(id);
            params.setCostAccounting(1);
            params.setAvailableInventory(1);
            params.setQuantityDecimal(0);
            params.setPriceDecimal(0);
            accountBookParametersRepository.save(params);
        }

        return params;
    }


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
        accountBookParameters.setUpdateTime(new Date());
        accountBookParametersRepository.save(accountBookParameters);
    }
}

