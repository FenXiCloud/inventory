package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMerchant is a Querydsl query type for Merchant
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMerchant extends EntityPathBase<Merchant> {

    private static final long serialVersionUID = -1010484899L;

    public static final QMerchant merchant = new QMerchant("merchant");

    public final StringPath address = createString("address");

    public final StringPath code = createString("code");

    public final DateTimePath<java.time.LocalDateTime> createDate = createDateTime("createDate", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final BooleanPath enabled = createBoolean("enabled");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath linkman = createString("linkman");

    public final StringPath mobile = createString("mobile");

    public final StringPath name = createString("name");

    public final DatePath<java.time.LocalDate> serviceEndDate = createDate("serviceEndDate", java.time.LocalDate.class);

    public final DatePath<java.time.LocalDate> serviceStartDate = createDate("serviceStartDate", java.time.LocalDate.class);

    public QMerchant(String variable) {
        super(Merchant.class, forVariable(variable));
    }

    public QMerchant(Path<? extends Merchant> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMerchant(PathMetadata metadata) {
        super(Merchant.class, metadata);
    }

}

