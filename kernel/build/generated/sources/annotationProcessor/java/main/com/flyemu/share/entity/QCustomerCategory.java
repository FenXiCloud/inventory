package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QCustomerCategory is a Querydsl query type for CustomerCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCustomerCategory extends EntityPathBase<CustomerCategory> {

    private static final long serialVersionUID = 1195437457L;

    public static final QCustomerCategory customerCategory = new QCustomerCategory("customerCategory");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final StringPath name = createString("name");

    public QCustomerCategory(String variable) {
        super(CustomerCategory.class, forVariable(variable));
    }

    public QCustomerCategory(Path<? extends CustomerCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerCategory(PathMetadata metadata) {
        super(CustomerCategory.class, metadata);
    }

}

