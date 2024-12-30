package com.flyemu.share.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.persistence.*;
import org.hibernate.annotations.Comment;

import java.io.Serializable;

/**
 * @功能描述: 菜单实体类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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

    @Comment("是否启用")
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

