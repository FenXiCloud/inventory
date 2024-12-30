package com.flyemu.share.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * @功能描述: 用户管理实体类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"mobile", "merchantId"})
})
@DynamicUpdate
public class Admin implements Serializable {
    private static final long serialVersionUID = 224044689322574706L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Comment("手机号码")
    @Column(nullable = false, length = 32)
    private String mobile;

    @Comment("用户名")
    @Column(nullable = false, length = 32, unique = true)
    private String username;

    @Comment("密码")
    @Column(nullable = false, length = 128)
    @JsonIgnore
    private String password;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("是否系统默认")
    private Boolean systemDefault;

    @Comment("最后登录时间")
    private LocalDateTime lastLoginDate;

    private Long roleId;

    @Column(nullable = false)
    private Long merchantId;

    public Admin(Long id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Admin admin = (Admin) o;
        return id.equals(admin.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

