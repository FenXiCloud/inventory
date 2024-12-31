package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

/**
 * @功能描述: 价格策略（销售采购价格取数优先级）
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
public class PricingStrategy implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String name;

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

    //客户等级售价 ： 取的是当前客户级别的商品档案定价
    public enum PriceSource {
        最近售价,客户等级售价, 最近采购价,预计采购价,最近销售成本
    }

    public enum PriceType {
       销售类取价,采购类取价, 异常成本处理
    }

}

