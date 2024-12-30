package com.flyemu.share.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * @功能描述: 商户管理后台管理员实体类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@DynamicUpdate
@NoArgsConstructor
public class MerchantUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Comment("用户名")
    @Column(nullable = false, length = 32, unique = true)
    private String username;

    @Comment("密码")
    @Column(nullable = false, length = 128)
    @JsonIgnore
    private String password;

    @Comment("是否启用")
    private Boolean enabled;

    @Comment("是否系统默认")
    private Boolean systemDefault;

    @Comment("最后登录时间")
    private LocalDateTime lastLoginDate;
}
