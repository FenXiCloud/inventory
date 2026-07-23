package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@DynamicInsert
public class AccountType implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String name;

    @Comment("收支类别")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, columnDefinition = "varchar(32)  default '收入'")
    private CostType costType;

    @Comment("父级ID")
    private Long pid;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum CostType {
        收入, 支出
    }

}

