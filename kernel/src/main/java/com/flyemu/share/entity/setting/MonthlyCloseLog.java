package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"closePeriod", "merchantId", "accountBookId"})
})
@Comment("结账日志表")
public class MonthlyCloseLog implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("结账期间（月末日期，如：2026-08-31）")
    @Column(nullable = false)
    private LocalDate closePeriod;

    @Comment("操作人ID")
    @Column(nullable = false)
    private Long operatorId;

    @Comment("操作人姓名")
    @Column(length = 50)
    private String operatorName;

    @Comment("操作时间")
    @Column(nullable = false)
    private LocalDateTime operateTime;

    @Comment("状态：1=正常结账，-1=反结账")
    @Column(nullable = false)
    private Integer status = 1;

    @Comment("备注")
    @Column(length = 255)
    private String remark;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @PrePersist
    public void prePersist() {
        this.operateTime = LocalDateTime.now();
    }
}
