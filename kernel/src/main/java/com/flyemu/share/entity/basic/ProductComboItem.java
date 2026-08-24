package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table
public class ProductComboItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("套餐ID")
    private Long comboId;

    @Comment("组件商品ID")
    private Long productId;

    @Comment("组件数量（以基本单位计）")
    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
