package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"name", "merchantId"})})
public class AccountBook implements Serializable {
    private static final long serialVersionUID = -24843859909377092L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("名称")
    @Column(length = 64, nullable = false)
    private String name;

    @Comment("结账日期")
    private LocalDate checkoutDate;

    @Comment("当前账套")
    private Boolean current;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("启用日期")
    @CreationTimestamp
    private LocalDate startDate;

    @Comment("默认仓库")
    private Long warehouseId;

    @Comment("币别")
    private String currency;

    @Column(nullable = false)
    private Long merchantId;

}

