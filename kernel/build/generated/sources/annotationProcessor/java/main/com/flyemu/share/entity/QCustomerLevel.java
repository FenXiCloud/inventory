package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCustomerLevel is a Querydsl query type for CustomerLevel
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCustomerLevel extends EntityPathBase<CustomerLevel> {

    private static final long serialVersionUID = -1152095279L;

    public static final QCustomerLevel customerLevel = new QCustomerLevel("customerLevel");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final StringPath name = createString("name");

    public QCustomerLevel(String variable) {
        super(CustomerLevel.class, forVariable(variable));
    }

    public QCustomerLevel(Path<? extends CustomerLevel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerLevel(PathMetadata metadata) {
        super(CustomerLevel.class, metadata);
    }

}

