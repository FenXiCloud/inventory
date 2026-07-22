package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
public class FinanceAccountLink implements TenantAware {

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
