package com.flyemu.share.common;

import com.flyemu.share.dto.AccountDto;

import java.util.function.Consumer;

/**
 * 将当前登录账套上下文写入 Query / 实体等租户字段。
 */
public final class TenantScope {

    private TenantScope() {
    }

    public static void bind(TenantAware target, AccountDto account) {
        target.setMerchantId(account.getMerchantId());
        target.setAccountBookId(account.getAccountBookId());
    }

    public static void bind(AccountDto account, Consumer<Long> setMerchantId, Consumer<Long> setAccountBookId) {
        setMerchantId.accept(account.getMerchantId());
        setAccountBookId.accept(account.getAccountBookId());
    }

    /** 仅写入商户 ID（账套级实体不适用时）。 */
    public static void bindMerchant(AccountDto account, Consumer<Long> setMerchantId) {
        setMerchantId.accept(account.getMerchantId());
    }
}
