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

/**
 * @功能描述: 账套管理
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
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

    @Column(nullable = false)
    private Long merchantId;


}

