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
@DynamicUpdate
@Comment("拣货单明细")
public class PickOrderItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("拣货单ID")
    @Column(nullable = false)
    private Long pickOrderId;

    @Comment("商品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("商品编码")
    @Column(length = 64)
    private String productCode;

    @Comment("商品名称")
    @Column(length = 255)
    private String productName;

    @Comment("规格")
    @Column(length = 120)
    private String specification;

    @Comment("单位")
    @Column(length = 32)
    private String unitName;

    @Comment("货位ID")
    private Long locationId;

    @Comment("货位编码")
    @Column(length = 32)
    private String locationCode;

    @Comment("应拣数量")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal planQuantity;

    @Comment("实拣数量")
    @Column(precision = 18, scale = 2)
    private BigDecimal actualQuantity;

    @Comment("是否整件：1=整件，0=零货")
    private Integer isCase;

    @Comment("整件数（如果是整件拣货）")
    @Column(precision = 18, scale = 2)
    private BigDecimal caseQuantity;

    @Comment("备注")
    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
