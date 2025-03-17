package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

/**
 * @功能描述: 关联云财务
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
public class FinanceRel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("财务软件Cookie")
    private String cookie;

    @Comment("财务软件账号")
    private String account;

    @Comment("财务软件密码")
    private String password;

    @Comment("财务软件URL")
    private String url;

    @Comment("财务软件账套ID")
    private Long accountSetsId;

    @Comment("财务软件账套名称")
    private String accountSetsName;

    @Comment("关联状态")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    @Comment("进销存账套名称")
    @Column(nullable = false)
    private String accountBookName;

    @Comment("进销存账套ID")
    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum State {
        未关联, 已关联
    }
}
