package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * @功能描述: 财务与进销存账套关联
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
public class FinanceAccountLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("财务软件账号")
    private String financeAccount;

    @Comment("进销存账套名称")
    @Column(nullable = false)
    private String accountBookName;

    @Comment("进销存账套ID")
    @Column(nullable = false)
    private Long accountBookId;

    @Comment("财务软件账套ID")
    private String financeAccountId;

    @Comment("财务软件账套名称")
    private String financeAccountName;

    @Comment("财务软件Cookie")
    private String financeCookie;

    @Comment("财务软件密码")
    private String financePassword;

    @Comment("关联状态")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LinkStatus linkStatus;

    @Comment("财务软件URL")
    private String url;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long merchantId;

    public enum LinkStatus {
        关联,    // 关联
        未关联   // 未关联
    }
}
