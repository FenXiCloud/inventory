package com.flyemu.share.entity.inventory;

import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.enums.OutboundType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @功能描述: 其他出库单
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
public class OtherOutbound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("客户ID")
    private Long customerId;

    @Comment("入库日期")
    @CreationTimestamp
    private Date inboundDate;

    @Comment("订单金额")
    private BigDecimal totalAmount;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("折后金额")
    private BigDecimal finalAmount;

    @Comment("已核销金额，客户id不为空时有效")
    private BigDecimal verifiedAmount;

    @Comment("备注")
    private String remarks;

    @Comment("出库类型")
    @Column(nullable = false,length = 32, columnDefinition = "varchar(20) default '其他出库'")
    @Enumerated(EnumType.STRING)
    private OutboundType outboundType;

    @Comment("订单状态")
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
