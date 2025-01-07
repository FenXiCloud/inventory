package com.flyemu.share.entity.basic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * @功能描述: 客户管理
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"merchantId", "accountBookId", "code"}),
})
@DynamicUpdate
@DynamicInsert
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("编码")
    private String code;

    @Comment("名称")
    @Column(length = 32, nullable = false)
    private String name;

    @Comment("联系人")
    private String contact;

    @Comment("电话")
    private String phone;

    @Comment("备注")
    private String remarks;

    @Comment("客户分类ID")
    @Column(nullable = false)
    private Long customerCategoryId;

    @Comment("客户等级ID")
    @Column(nullable = false)
    private Long customerLevelId;

    @Comment("客户余额,应收账款")
    private BigDecimal balance;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
