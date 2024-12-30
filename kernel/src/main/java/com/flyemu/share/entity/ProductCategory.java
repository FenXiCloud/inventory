package com.flyemu.share.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

/**
 * @功能描述: 商品分类
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint(name = "uc_product_category_code", columnNames = {"merchantId", "accountBookId", "code"}),
})
public class ProductCategory {

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

    @Comment("排序号")
    private Long sort;

    @Comment("是否叶子节点")
    @ColumnDefault("b'1'")
    @Column(nullable = false)
    private Boolean leaf;

    @Column( nullable = false)
    private Long accountBookId;

    @Column( nullable = false)
    private Long merchantId;
}
