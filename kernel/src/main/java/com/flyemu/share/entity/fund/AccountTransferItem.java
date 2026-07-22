package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class AccountTransferItem implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("转出账户ID")
    private Long fromAccountId;
    @Comment("转出账户名称")
    private String fromAccountName;

    @Comment("转入账户ID")
    private Long toAccountId;

    @Comment("转账单id")
    private Long accountTransferId;
    @Comment("转入账户名称")
    private String toAccountName;
    @Comment("结算号")
    private String settlementNumber;
    @Comment("金额")
    private BigDecimal amount;

    @Comment("结算方式")
    private Long paymentMethodId;
    @Comment("结算方式名称")
    private String  paymentMethodName;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
