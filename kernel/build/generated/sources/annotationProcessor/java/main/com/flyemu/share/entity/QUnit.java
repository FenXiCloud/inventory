package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QUnit is a Querydsl query type for Unit
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUnit extends EntityPathBase<Unit> {

    private static final long serialVersionUID = -16557159L;

    public static final QUnit unit = new QUnit("unit");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final StringPath name = createString("name");

    public QUnit(String variable) {
        super(Unit.class, forVariable(variable));
    }

    public QUnit(Path<? extends Unit> path) {
        super(path.getType(), path.getMetadata());
    }

    public QUnit(PathMetadata metadata) {
        super(Unit.class, metadata);
    }

}

