package com.flyemu.share.entity.sales;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @功能描述: 销售产品价格记录
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
public class SalesPriceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("销售出库主表ID")
    private Long salesOutboundId;

    @Comment("销售日期")
    @CreationTimestamp
    private Date salesDate;

    @Comment("产品ID")
    private Long productId;

    @Comment("客户ID")
    private Long customerId;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("数量（以基本单位计）")
    private Double quantity;

    @Comment("辅助单位ID(可为空)")
    private Long secondaryUnitId;

    @Comment("辅助单位数量 (可为空")
    private Double secondaryQuantity;

    @Comment("换算率 (基本单位到辅助单位的换算率，例如：1箱=12个，则换算率为12。如果未使用辅助单位，则为1)")
    private BigDecimal conversionRate;

    @Comment("单价（以基本单位计）")
    private BigDecimal unitPrice;

    @Comment("单价（辅助单位）")
    private BigDecimal secondaryUnitPrice;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
