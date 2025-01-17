package com.flyemu.share.entity.fund;

import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @功能描述: 其他收入单
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
public class OtherIncome {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("客户ID")
    private Long customerId;

    @Comment("单据日期")
    private LocalDate orderDate;

    @Comment("单据编号")
    private String orderNo;

    @Comment("金额")
    private BigDecimal amount;

    @Comment("已核销金额")
    private BigDecimal verifiedAmount;

    @Comment("收款金额")
    private BigDecimal collectionAmount;

    @Comment("收款账户ID")
    private Long accountId;

    @Comment("状态")
    @Column(nullable = false,length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
