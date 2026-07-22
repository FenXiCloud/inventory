package com.flyemu.share.entity.setting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.io.Serializable;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Menu implements Serializable {
    private static final long serialVersionUID = 895088922873812109L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("组件")
    private String component;

    @Comment("菜单名称")
    private String name;

    @Comment("图标")
    private String iconCls;

    @Comment("是否要求权限")
    private Boolean requireAuth;

    @Comment("父id")
    private Long parentId;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("位置")
    private Integer pos;

    @Enumerated(EnumType.STRING)
    @Column(length = 32,columnDefinition = "varchar(32)  default 'MERCHANT'")
    private MenuModule menuModule;

    @Enumerated(EnumType.STRING)
    private MenuType menuType;

    @Enumerated(EnumType.STRING)
    private MenuGroup menuGroup;

    /**
     * 菜单模块，MERCHANT
     */
    public enum MenuModule{
        MERCHANT
    }

    /**
     * 菜单类型 功能 菜单
     */
    public enum MenuType {
        FUNCTION, MENU
    }

    /**
     * 菜单分组，商家、门店
     */
    public enum MenuGroup {
        MERCHANT
    }
}

