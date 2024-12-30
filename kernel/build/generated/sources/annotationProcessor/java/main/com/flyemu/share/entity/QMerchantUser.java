package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMerchantUser is a Querydsl query type for MerchantUser
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMerchantUser extends EntityPathBase<MerchantUser> {

    private static final long serialVersionUID = -2117623096L;

    public static final QMerchantUser merchantUser = new QMerchantUser("merchantUser");

    public final BooleanPath enabled = createBoolean("enabled");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastLoginDate = createDateTime("lastLoginDate", java.time.LocalDateTime.class);

    public final StringPath name = createString("name");

    public final StringPath password = createString("password");

    public final BooleanPath systemDefault = createBoolean("systemDefault");

    public final StringPath username = createString("username");

    public QMerchantUser(String variable) {
        super(MerchantUser.class, forVariable(variable));
    }

    public QMerchantUser(Path<? extends MerchantUser> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMerchantUser(PathMetadata metadata) {
        super(MerchantUser.class, metadata);
    }

}

