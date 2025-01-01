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
 * @功能描述: 库存汇总表
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
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("产品ID")
    private Long productId;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("基础单位ID")
    private Long baseUnitId;

    @Comment("库存数量")
    private Integer currentQuantity;

    @Comment("安全库存")
    private Integer safetyStock;

    @Comment("平均成本")
    private BigDecimal averageCost;

    @Comment("成本总计")
    private BigDecimal totalCost;

    @Comment("更新时间")
    private LocalDateTime updatedAt;
}
