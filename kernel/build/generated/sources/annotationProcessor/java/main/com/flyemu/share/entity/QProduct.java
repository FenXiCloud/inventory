package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = 644060858L;

    public static final QProduct product = new QProduct("product");

    public final NumberPath<Long> accountBookId = createNumber("accountBookId", Long.class);

    public final NumberPath<Long> categoryId = createNumber("categoryId", Long.class);

    public final StringPath code = createString("code");

    public final DateTimePath<java.util.Date> createDate = createDateTime("createDate", java.util.Date.class);

    public final BooleanPath enabled = createBoolean("enabled");

    public final BooleanPath enableMultiUnit = createBoolean("enableMultiUnit");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath imgPath = createString("imgPath");

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public final ListPath<com.flyemu.share.dto.UnitPrice, SimplePath<com.flyemu.share.dto.UnitPrice>> multiUnit = this.<com.flyemu.share.dto.UnitPrice, SimplePath<com.flyemu.share.dto.UnitPrice>>createList("multiUnit", com.flyemu.share.dto.UnitPrice.class, SimplePath.class, PathInits.DIRECT2);

    public final StringPath name = createString("name");

    public final StringPath pinyin = createString("pinyin");

    public final StringPath remark = createString("remark");

    public final NumberPath<Integer> sort = createNumber("sort", Integer.class);

    public final StringPath specification = createString("specification");

    public final NumberPath<Long> unitId = createNumber("unitId", Long.class);

    public QProduct(String variable) {
        super(Product.class, forVariable(variable));
    }

    public QProduct(Path<? extends Product> path) {
        super(path.getType(), path.getMetadata());
    }

    public QProduct(PathMetadata metadata) {
        super(Product.class, metadata);
    }

}

