package com.flyemu.share.entity.inventory;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"orderNo", "merchantId", "accountBookId"})
})
@Comment("拣货单")
public class PickOrder implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("拣货单号")
    @Column(length = 64, nullable = false)
    private String orderNo;

    @Comment("关联单据类型（销售出库/采购退货等）")
    @Column(length = 32, nullable = false)
    private String sourceType;

    @Comment("关联单据ID")
    @Column(nullable = false)
    private Long sourceId;

    @Comment("关联单据编号")
    @Column(length = 64)
    private String sourceNo;

    @Comment("拣货日期")
    @Column(nullable = false)
    private LocalDate pickDate;

    @Comment("仓库ID")
    @Column(nullable = false)
    private Long warehouseId;

    @Comment("货位类型：WHOLE=整货区，ZERO=零货区")
    @Column(length = 20, nullable = false)
    private String locationType;

    @Comment("货位ID")
    private Long locationId;

    @Comment("状态：0=待拣货，1=拣货中，2=已完成")
    @Column(nullable = false)
    private Integer status = 0;

    @Comment("拣货人")
    private Long pickerId;

    @Comment("拣货时间")
    private LocalDateTime pickTime;

    @Comment("备注")
    @Column(length = 255)
    private String remark;

    @Comment("创建人")
    @Column(nullable = false)
    private Long createdBy;

    @Comment("创建时间")
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Transient
    private List<PickOrderItem> items;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
