package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table
public class PriceRecord implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据主表ID")
    private Long orderId;

    @Comment("单据日期")
    @CreationTimestamp
    private Date orderDate;

    @Comment("产品ID")
    private Long productId;

    @Comment("货商ID")
    private Long supplierId;

    @Comment("客户ID")
    private Long customerId;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("数量（以基本单位计）")
    private Double quantity;

    @Comment("单价（以基本单位计）")
    private BigDecimal unitPrice;

    @Comment("辅助单位价格")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    @Comment("价格类别")
    @Enumerated(EnumType.STRING)
    private PriceType priceType;

    @Comment("价格来源")
    @Enumerated(EnumType.STRING)
    private PriceSource priceSource;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
