package com.flyemu.share.entity.fund;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @功能描述: 付款单明细
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
@DynamicUpdate
public class OrderPaymentItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("订单ID")
    private Long orderId;

    @Comment("业务类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Comment("单据日期")
    @CreationTimestamp
    private Date orderDate;

    @Comment("单据金额")
    private BigDecimal orderAmount;

    @Comment("已核销金额")
    private BigDecimal verifiedAmount;

    @Comment("本次核销金额")
    private BigDecimal oneTimeVerifiedAmount;

    public enum OrderType {
        采购入库,采购退货,其他支出单
    }
}
