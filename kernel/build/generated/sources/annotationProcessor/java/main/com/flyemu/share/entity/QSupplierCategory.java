package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QSupplierCategory is a Querydsl query type for SupplierCategory
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSupplierCategory extends EntityPathBase<SupplierCategory> {

    private static final long serialVersionUID = -1777231489L;

    public static final QSupplierCategory supplierCategory = new QSupplierCategory("supplierCategory");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final StringPath name = createString("name");

    public QSupplierCategory(String variable) {
        super(SupplierCategory.class, forVariable(variable));
    }

    public QSupplierCategory(Path<? extends SupplierCategory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSupplierCategory(PathMetadata metadata) {
        super(SupplierCategory.class, metadata);
    }

}

