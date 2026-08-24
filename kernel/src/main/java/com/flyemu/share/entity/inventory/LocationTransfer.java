package com.flyemu.share.entity.inventory;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"orderNo", "merchantId", "accountBookId"})
})
@Comment("货位调拨单")
public class LocationTransfer implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据编号")
    @Column(length = 64, nullable = false)
    private String orderNo;

    @Comment("调拨日期")
    @Column(nullable = false)
    private LocalDate transferDate;

    @Comment("源仓库ID")
    @Column(nullable = false)
    private Long fromWarehouseId;

    @Comment("源货位ID")
    @Column(nullable = false)
    private Long fromLocationId;

    @Comment("目标仓库ID")
    @Column(nullable = false)
    private Long toWarehouseId;

    @Comment("目标货位ID")
    @Column(nullable = false)
    private Long toLocationId;

    @Comment("商品ID")
    @Column(nullable = false)
    private Long productId;

    @Comment("调拨数量（基本单位）")
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal quantity;

    @Comment("调拨整件数")
    @Column(precision = 18, scale = 2)
    private BigDecimal caseQuantity;

    @Comment("备注")
    @Column(length = 255)
    private String remark;

    @Comment("单据状态")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.已保存;

    @Comment("创建人")
    @Column(nullable = false)
    private Long createdBy;

    @Comment("创建时间")
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
