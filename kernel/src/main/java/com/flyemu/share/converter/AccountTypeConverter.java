package com.flyemu.share.converter;

import com.flyemu.share.entity.basic.Account;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * 账户类别枚举转换器，优雅处理数据库中可能存在的非法值。
 */
@Slf4j
@Converter(autoApply = true)
public class AccountTypeConverter implements AttributeConverter<Account.AccountType, String> {

    @Override
    public String convertToDatabaseColumn(Account.AccountType attribute) {
        if (attribute == null) {
            return Account.AccountType.资产.name();
        }
        return attribute.name();
    }

    @Override
    public Account.AccountType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isEmpty()) {
            return Account.AccountType.资产;
        }
        try {
            return Account.AccountType.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            log.warn("数据库中存在无效的账户类别值: '{}', 已默认转换为'资产'", dbData);
            return Account.AccountType.资产;
        }
    }
}
