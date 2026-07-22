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
@Table
@DynamicUpdate
public class OrderPayment implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("货商ID")
    private Long supplierId;
    @Comment("货商名称")
        private String supplierName;
    @Comment("单据类型,1付款单,2预付款单")
    private Integer orderType;
    @Comment("单据日期")
    private LocalDate orderDate;

    @Comment("单据编号")
    private String orderNo;
    @Comment("单据来源,1进销存")
    private String documentSource;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("付款金额")
    private BigDecimal collectionAmount;

    @Comment("总欠款")
    private BigDecimal totalAmountsOwed;

    @Comment("本次核销金额")
    private BigDecimal verificationAmount;
    @Comment("预付款金额")
    private BigDecimal advanceCollectionsAmount;
    @Comment("本单应该核销金额")
    private BigDecimal shouldVerificationAmount;
    @Comment("本单已核销金额")
    private BigDecimal hasVerificationAmount;
    @Comment("本单未核销金额")
    private BigDecimal notVerificationAmount;
    @Comment("核销状态,0未核销,1部分核销,2全部核销")
    private Integer writeOffStatus;

    @Comment("状态")
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Comment("业务员ID")
    private Long orderStaffId;
    @Comment("业务员名称")
    private String orderStaffName;

    @Comment("创建人")
    private Long createdBy;
    @Comment("最后修改人")
    @Column(name = "update_by")
    private Long updatedBy;
    @Comment("创建时间")
    private LocalDateTime createdAt;
    @Comment("最后修改时间")
    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("备注")
    private String remarks;

}
