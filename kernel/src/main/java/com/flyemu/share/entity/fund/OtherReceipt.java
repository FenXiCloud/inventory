package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import com.fasterxml.jackson.annotation.JsonInclude;
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

@JsonInclude()
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "other_income")
@DynamicUpdate
public class OtherReceipt implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Comment("结算方式名称")
    private String settlementAccount;
    @Comment("结算方式ID")
    private Long settlementAccountId;
    @Comment("客户ID")
    private Long customerId;
    @Comment("客户名称")
    private String customerName;
    @Comment("备注")
    private String remarks;
    @Comment("单据日期")
    private LocalDate orderDate;
    @Comment("单据编号")
    private String orderNo;
    @Comment("欠款金额")
    private BigDecimal arrearsAmount;
    @Comment("收款到期日")
    private LocalDate expirationDate;
    @Comment("收款金额")
    private BigDecimal collectionAmount;
    @Comment("状态")
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
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

    @Comment("业务员ID")
    private Long orderStaffId;
    @Comment("业务员名称")
    private String orderStaffName;
    @Column(nullable = false)
    private Long accountBookId;
    @Comment("最后修改人")
    @Column(name = "update_by")
    private Long updatedBy;
    @Comment("最后修改时间")
    @Column(name = "update_at")
    private LocalDateTime updatedAt;
    @Column(nullable = false)
    private Long merchantId;
}
