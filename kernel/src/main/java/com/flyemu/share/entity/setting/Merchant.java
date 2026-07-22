package com.flyemu.share.entity.setting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@ToString
public class Merchant implements Serializable {
    private static final long serialVersionUID = 952729070027371070L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("商户编码")
    private String code;

    @Comment("商户名称")
    private String name;

    @Comment("地址")
    private String address;

    @Comment("创建日期")
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Comment("联系人")
    private String contact;

    @Comment("邮箱")
    private String email;

    @Comment("电话号码")
    private String mobile;

    @Comment("存货计价方式")
    private CostingMethod costingMethod;

    @Comment("允许负库存")
    private Boolean stockField;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    public enum CostingMethod {
        移动平均法,先进先出法
    }

}

