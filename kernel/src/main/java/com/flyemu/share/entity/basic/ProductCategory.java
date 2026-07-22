package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint( columnNames = {"merchantId", "accountBookId", "code"}),
})
public class ProductCategory implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("分类编码")
    @Column(length = 64, nullable = false)
    private String code;

    @Comment("分类名称")
    @Column(length = 64, nullable = false)
    private String name;

    @Comment("父级ID")
    private Long pid;

    private String path;

    private String imgPath;

    @Comment("排序号")
    private Long sort;

    @Comment("是否末级（分类下有产品时为 true，无产品时可继续创建下级）")
    @ColumnDefault("b'0'")
    @Column(nullable = false)
    private Boolean leaf;

    @Column( nullable = false)
    private Long accountBookId;

    @Column( nullable = false)
    private Long merchantId;
}
