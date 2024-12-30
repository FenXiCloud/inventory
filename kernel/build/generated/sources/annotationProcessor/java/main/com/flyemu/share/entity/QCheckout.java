package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCheckout is a Querydsl query type for Checkout
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCheckout extends EntityPathBase<Checkout> {

    private static final long serialVersionUID = 1031716059L;

    public static final QCheckout checkout = new QCheckout("checkout");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final DatePath<java.time.LocalDate> checkDate = createDate("checkDate", java.time.LocalDate.class);

    public final NumberPath<Long> checkId = createNumber("checkId", Long.class);

    public final DateTimePath<java.time.LocalDateTime> createDate = createDateTime("createDate", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public QCheckout(String variable) {
        super(Checkout.class, forVariable(variable));
    }

    public QCheckout(Path<? extends Checkout> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCheckout(PathMetadata metadata) {
        super(Checkout.class, metadata);
    }

}

