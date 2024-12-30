package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QWarehouse is a Querydsl query type for Warehouse
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWarehouse extends EntityPathBase<Warehouse> {

    private static final long serialVersionUID = -478560818L;

    public static final QWarehouse warehouse = new QWarehouse("warehouse");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final StringPath name = createString("name");

    public final BooleanPath systemDefault = createBoolean("systemDefault");

    public QWarehouse(String variable) {
        super(Warehouse.class, forVariable(variable));
    }

    public QWarehouse(Path<? extends Warehouse> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWarehouse(PathMetadata metadata) {
        super(Warehouse.class, metadata);
    }

}

