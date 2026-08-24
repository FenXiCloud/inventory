package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table
public class ProductCombo implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("套餐编码")
    @Column(length = 64)
    private String code;

    @Comment("套餐名称")
    @Column(length = 255, nullable = false)
    private String name;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    private Boolean enabled;

    @Comment("备注")
    private String remarks;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
