package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QCustomerLevelPrice is a Querydsl query type for CustomerLevelPrice
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCustomerLevelPrice extends EntityPathBase<CustomerLevelPrice> {

    private static final long serialVersionUID = 955781112L;

    public static final QCustomerLevelPrice customerLevelPrice = new QCustomerLevelPrice("customerLevelPrice");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> customerLevelId = createNumber("customerLevelId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final NumberPath<java.math.BigDecimal> price = createNumber("price", java.math.BigDecimal.class);

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    public final NumberPath<Long> unitId = createNumber("unitId", Long.class);

    public final ListPath<com.flyemu.share.dto.UnitPrice, SimplePath<com.flyemu.share.dto.UnitPrice>> unitPrice = this.<com.flyemu.share.dto.UnitPrice, SimplePath<com.flyemu.share.dto.UnitPrice>>createList("unitPrice", com.flyemu.share.dto.UnitPrice.class, SimplePath.class, PathInits.DIRECT2);

    public QCustomerLevelPrice(String variable) {
        super(CustomerLevelPrice.class, forVariable(variable));
    }

    public QCustomerLevelPrice(Path<? extends CustomerLevelPrice> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCustomerLevelPrice(PathMetadata metadata) {
        super(CustomerLevelPrice.class, metadata);
    }

}

