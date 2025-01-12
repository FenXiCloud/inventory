package com.flyemu.share.entity.fund;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @功能描述: 账户管理
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(uniqueConstraints = {
        @UniqueConstraint( columnNames = {"merchantId", "accountBookId", "name"})
})
@DynamicUpdate
@DynamicInsert
public class Account implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("名称")
    @Column(length = 32, nullable = false)
    private String name;

    @Comment("是否系统默认")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean systemDefault;

    @Comment("账户类别")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, columnDefinition = "varchar(32)  default '资产'")
    private Account.AccountType accountType;

    @Comment("账户类别名称")
    @Enumerated(EnumType.STRING)
    @Column(length = 32, columnDefinition = "varchar(32)  default '银行账户'")
    private Account.AccountTypeItem accountTypeItem;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("币别")
    private String currency;

    @Comment("账户余额")
    private BigDecimal balance;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum AccountType {
        资产, 负债
    }

    public enum AccountTypeItem {
        现金账户, 银行账户, 虚拟账户, 数字货币, 信用账户
    }

}

