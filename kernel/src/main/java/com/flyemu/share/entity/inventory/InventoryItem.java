package com.flyemu.share.entity.inventory;

import com.flyemu.share.enums.OperationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @功能描述: 库存明细表
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
public class InventoryItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("关联单据ID")
    private Long orderId;

    @Comment("产品ID")
    private Long productId;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("操作类型：期初、入库、出库、调拨")
    private OperationType operationType;

    @Comment("正数为入库，负数为出库")
    private Integer quantity;

    @Comment("基础单位ID")
    private Long baseUnitId;

    @Comment("批次号")
    private String batchNumber;

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
