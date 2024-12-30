package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * @功能描述: 组织机构
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

    @Column(length = 64, nullable = false)
    private String name;

    private LocalDate checkoutDate;

    //当前账套
    private Boolean current;

    private Boolean enabled;

    @CreationTimestamp
    private LocalDate startDate;

    @Column(nullable = false)
    private Long merchantId;


}
