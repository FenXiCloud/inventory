package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
public class FinanceRel implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("财务软件Cookie")
    private String cookie;

    @Comment("财务软件账号")
    private String account;

    @Comment("财务软件密码")
    @Convert(converter = com.flyemu.share.crypto.EncryptedStringConverter.class)
    @Column(length = 256)
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
