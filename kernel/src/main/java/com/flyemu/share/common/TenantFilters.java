package com.flyemu.share.common;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.NumberPath;

/**
 * QueryDSL 租户条件拼装，供各 Service.Query 复用。
 */
public final class TenantFilters {

    private TenantFilters() {
    }

    public static void merchant(BooleanBuilder builder, NumberPath<Long> path, Long merchantId) {
        if (merchantId != null) {
            builder.and(path.eq(merchantId));
        }
    }

    public static void accountBook(BooleanBuilder builder, NumberPath<Long> path, Long accountBookId) {
        if (accountBookId != null) {
            builder.and(path.eq(accountBookId));
        }
    }

    public static void eq(BooleanBuilder builder,
                          NumberPath<Long> merchantPath, Long merchantId,
                          NumberPath<Long> accountBookPath, Long accountBookId) {
        merchant(builder, merchantPath, merchantId);
        accountBook(builder, accountBookPath, accountBookId);
    }
}
