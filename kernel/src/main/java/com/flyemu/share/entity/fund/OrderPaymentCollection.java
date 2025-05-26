package com.flyemu.share.entity.fund;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * (OrderPaymentCollection)实体类
 *
 * @author shuaiqi
 * @since 2025-05-20 11:29:36
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class OrderPaymentCollection implements Serializable {
    @Serial
    private static final long serialVersionUID = -45726432745131115L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /**
     * 收款单ID
     */
    @Comment("收款单ID")
    private Integer paymentId;
    /**
     * 结算账户 cash现金，bank_deposit银行，wechat_pay微信，alipay支付宝
     */
    @Comment("结算账户 cash现金，bank_deposit银行，wechat_pay微信，alipay支付宝")
    private String settlementAccount;
    /**
     * 结算方式
     */
    private Integer paymentMethodId;
    @Comment("结算方式名称")
    private String paymentMethodName;
    /**
     * 收款金额
     */
    @Comment("金额")
    private BigDecimal amount;
    /**
     * 备注
     */
    @Comment("备注")
    private String remarks;
    /**
     * 在线交易单号
     */
    @Comment("结算号")
    private String theOnlineTransactionNumber;

}

