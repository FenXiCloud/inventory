package com.flyemu.share.entity.basic;

import com.flyemu.share.dto.AuxiliaryUnitPrice;
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
import java.util.Date;
import java.util.List;

/**
 * @功能描述: 产品价格记录
 * @创建时间: 2024年12月31日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table
public class PriceRecord {

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
    @Column(length = 32, columnDefinition = "varchar(32)  default '入库'")
    private PriceType priceType;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum PriceType {
        入库, 出库
    }

}
