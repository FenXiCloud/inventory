package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"warehouseId", "code", "merchantId", "accountBookId"})
})
@Comment("仓库货位表")
public class WarehouseLocation implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("所属仓库ID")
    @Column(nullable = false)
    private Long warehouseId;

    @Comment("货位编码")
    @Column(length = 32, nullable = false)
    private String code;

    @Comment("货位名称")
    @Column(length = 64, nullable = false)
    private String name;

    @Comment("货位类型：WHOLE=整货，ZERO=零货")
    @Column(length = 20, nullable = false)
    private String type;

    @Comment("容量")
    @Column(precision = 18, scale = 2)
    private BigDecimal capacity;

    @Comment("当前库存量")
    @Column(precision = 18, scale = 2)
    private BigDecimal currentQty = BigDecimal.ZERO;

    @Comment("状态：1=启用，0=禁用")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("排序")
    private Integer sort;

    @Comment("备注")
    @Column(length = 255)
    private String remark;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
