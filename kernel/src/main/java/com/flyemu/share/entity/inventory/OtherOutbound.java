package com.flyemu.share.entity.inventory;

import com.flyemu.share.common.TenantAware;
import com.fasterxml.jackson.annotation.JsonFormat;
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

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class OtherOutbound implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("客户ID")
    private Long customerId;

    @Comment("入库日期")
    @CreationTimestamp
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd")
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

    @Comment("盘点主表ID")
    private Long stockTakeId;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
