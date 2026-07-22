package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.enums.PolicySource;
import com.flyemu.share.enums.PolicyType;
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

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class PricingPolicy implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("优先级")
    private Integer priority;

    @Comment("取数类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicyType policyType;

    @Comment("取数来源")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private PolicySource policySource;

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

