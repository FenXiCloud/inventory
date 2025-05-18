package com.flyemu.share.service.setting;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Assert;
import com.blazebit.persistence.querydsl.BlazeJPAQuery;
import com.flyemu.share.entity.setting.AccountBookParameters;
import com.flyemu.share.entity.setting.QAccountBookParameters;
import com.flyemu.share.repository.AccountBookParametersRepository;
import com.flyemu.share.service.AbsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


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

    @Transactional
    public AccountBookParameters list(Integer id) {
        Assert.notNull(id, "id不能为空");
        AccountBookParameters accountBookParameters = bqf
                .selectFrom(Q_ACCOUNT_BOOK_PARAMETERS)
                .where(Q_ACCOUNT_BOOK_PARAMETERS.accountBookId.eq(id))
                .fetchFirst();

        if (accountBookParameters == null) {
            accountBookParameters = new AccountBookParameters();
            accountBookParameters.setAccountBookId(id);
            accountBookParameters.setCostAccounting(1);
            accountBookParameters.setAvailableInventory(1);
            accountBookParameters.setQuantityDecimal(0);
            accountBookParameters.setPriceDecimal(0);
            accountBookParametersRepository.save(accountBookParameters);

            // 重新查询以确保返回最新数据
            accountBookParameters = bqf
                    .selectFrom(Q_ACCOUNT_BOOK_PARAMETERS)
                    .where(Q_ACCOUNT_BOOK_PARAMETERS.accountBookId.eq(id))
                    .fetchFirst();
        }

        return accountBookParameters;
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

