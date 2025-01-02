package com.flyemu.share.entity.inventory;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @功能描述: 成本调整明细表
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
public class CostAdjustmentItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("成本调整单主表")
    private Long costAdjustmentId;

    @Comment("产品ID")
    private Long productId;

    @Comment("基本单位ID")
    private Long baseUnitId;

    @Comment("库存数量")
    private Integer currentQuantity;

    @Comment("调整前的产品总成本")
    private BigDecimal totalCost;

    @Comment("调整金额：本次调整的总金额（调整数量 * (调整后成本 - 调整前成本)）")
    private BigDecimal adjustmentAmount;

    @Comment("备注")
    private String remarks;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
