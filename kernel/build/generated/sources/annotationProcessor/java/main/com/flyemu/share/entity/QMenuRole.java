package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMenuRole is a Querydsl query type for MenuRole
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMenuRole extends EntityPathBase<MenuRole> {

    private static final long serialVersionUID = -1109020150L;

    public static final QMenuRole menuRole = new QMenuRole("menuRole");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> menuId = createNumber("menuId", Long.class);

    public final NumberPath<Long> roleId = createNumber("roleId", Long.class);

    public QMenuRole(String variable) {
        super(MenuRole.class, forVariable(variable));
    }

    public QMenuRole(Path<? extends MenuRole> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMenuRole(PathMetadata metadata) {
        super(MenuRole.class, metadata);
    }

}

