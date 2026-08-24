package com.flyemu.share.entity.inventory;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 商品序列号（每个商品实例一条记录，支持出入库追溯）。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class ProductSerial implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("序列号")
    @Column(length = 128, nullable = false)
    private String serialNumber;

    @Comment("产品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("批次号")
    @Column(length = 64)
    private String batchNumber;

    @Comment("状态: 在库/已出库/报废")
    @Column(length = 16)
    private String status;

    @Comment("入库日期")
    private LocalDate inboundDate;

    @Comment("出库日期")
    private LocalDate outboundDate;

    @Comment("入库单ID")
    private Long inboundOrderId;

    @Comment("出库单ID")
    private Long outboundOrderId;

    @Comment("备注")
    private String remark;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("创建时间")
    private LocalDateTime createdAt;
}
