package com.flyemu.share.entity.basic;

import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * @功能描述: 价格取数规则(销售采购价格取数优先级）
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class PricingPolicy implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("优先级")
    private Integer priority;

    @Comment("价格类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PriceType priceType;

    @Comment("价格来源")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PriceSource priceSource;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}

