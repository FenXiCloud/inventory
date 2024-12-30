package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QAccountBook is a Querydsl query type for AccountBook
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAccountBook extends EntityPathBase<AccountBook> {

    private static final long serialVersionUID = 361645825L;

    public static final QAccountBook accountBook = new QAccountBook("accountBook");

    public final DatePath<java.time.LocalDate> checkoutDate = createDate("checkoutDate", java.time.LocalDate.class);

    public final BooleanPath current = createBoolean("current");

    public final BooleanPath enabled = createBoolean("enabled");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final StringPath name = createString("name");

    public final DatePath<java.time.LocalDate> startDate = createDate("startDate", java.time.LocalDate.class);

    public QAccountBook(String variable) {
        super(AccountBook.class, forVariable(variable));
    }

    public QAccountBook(Path<? extends AccountBook> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAccountBook(PathMetadata metadata) {
        super(AccountBook.class, metadata);
    }

}

