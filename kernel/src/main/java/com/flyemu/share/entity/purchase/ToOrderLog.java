package com.flyemu.share.entity.purchase;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 以销定购操作日志
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
public class ToOrderLog implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("关联的销售订单ID")
    private Long saleOrderId;

    @Comment("生成的采购入库单ID")
    private Long purchaseInId;

    @Comment("商品ID")
    private Long goodsId;

    @Comment("本次采购数量")
    @Column(precision = 15, scale = 4)
    private BigDecimal purchaseQuantity;

    @Comment("本次采购单价")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal purchasePrice;

    @Comment("供应商ID")
    private Long supplierId;

    @Comment("操作人")
    private Long createdBy;

    @Comment("操作时间")
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
