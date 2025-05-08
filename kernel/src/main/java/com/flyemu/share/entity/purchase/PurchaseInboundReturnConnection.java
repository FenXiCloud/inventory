package com.flyemu.share.entity.purchase;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * @功能描述: 采购入库单退货单关联表
 * @创建时间: 2025年05月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "PurchaseInboundReturnConnection", indexes = {
        @Index(columnList = "purchaseReturnId"),
        @Index(columnList = "purchaseInboundId")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"purchaseReturnId", "purchaseInboundId"})
})
@DynamicUpdate
public class PurchaseInboundReturnConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("退货单ID")
    private Long purchaseReturnId;

    @Comment("采购入库主表ID")
    private Long purchaseInboundId;

    @Comment("创建时间")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
