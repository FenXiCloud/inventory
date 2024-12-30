package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMerchantMenu is a Querydsl query type for MerchantMenu
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMerchantMenu extends EntityPathBase<MerchantMenu> {

    private static final long serialVersionUID = -2117874596L;

    public static final QMerchantMenu merchantMenu = new QMerchantMenu("merchantMenu");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> menuId = createNumber("menuId", Long.class);

    public final NumberPath<Long> merchantId = createNumber("merchantId", Long.class);

    public QMerchantMenu(String variable) {
        super(MerchantMenu.class, forVariable(variable));
    }

    public QMerchantMenu(Path<? extends MerchantMenu> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMerchantMenu(PathMetadata metadata) {
        super(MerchantMenu.class, metadata);
    }

}

