package com.flyemu.share.entity.inventory;

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
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class AssemblyOrderItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("组装拆卸单主表ID")
    private Long assemblyOrderId;

    @Comment("组件商品ID")
    private Long productId;

    @Comment("数量（以基本单位计）")
    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Comment("组件成本金额（审核时写入，反审核回补用）")
    private BigDecimal costAmount;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
