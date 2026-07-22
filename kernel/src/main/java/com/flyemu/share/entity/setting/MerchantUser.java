package com.flyemu.share.entity.setting;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
@Getter
@Setter
@Entity
@DynamicUpdate
@NoArgsConstructor
public class MerchantUser implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("姓名")
    @Column(nullable = false)
    private String name;

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
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean systemDefault;

    @Comment("最后登录时间")
    private LocalDateTime lastLoginDate;
}
