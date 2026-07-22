package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"merchantId","accountBookId", "productId", "customerLevelId"})
})
public class CustomerLevelPrice implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("产品ID")
    private Long productId;

    @Comment("客户等级ID")
    private Long customerLevelId;

    @Comment("基础单位")
    private Long unitId;

    @Comment("基础单位价格")
    private BigDecimal price;

    @Comment("客户单位辅助价格")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}

