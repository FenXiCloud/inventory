package com.flyemu.share.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QMenu is a Querydsl query type for Menu
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMenu extends EntityPathBase<Menu> {

    private static final long serialVersionUID = -16803980L;

    public static final QMenu menu = new QMenu("menu");

    public final StringPath component = createString("component");

    public final BooleanPath enabled = createBoolean("enabled");

    public final StringPath iconCls = createString("iconCls");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<Menu.MenuGroup> menuGroup = createEnum("menuGroup", Menu.MenuGroup.class);

    public final EnumPath<Menu.MenuModule> menuModule = createEnum("menuModule", Menu.MenuModule.class);

    public final EnumPath<Menu.MenuType> menuType = createEnum("menuType", Menu.MenuType.class);

    public final StringPath name = createString("name");

    public final NumberPath<Long> parentId = createNumber("parentId", Long.class);

    public final NumberPath<Integer> pos = createNumber("pos", Integer.class);

    public final BooleanPath requireAuth = createBoolean("requireAuth");

    public QMenu(String variable) {
        super(Menu.class, forVariable(variable));
    }

    public QMenu(Path<? extends Menu> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMenu(PathMetadata metadata) {
        super(Menu.class, metadata);
    }

}

