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
 * @author q
 * @功能描述: 收款单
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
public class OrderReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("客户ID")
    private Long customerId;
    @Comment("客户名称")
    private String customerName;
    @Comment("单据类型,1收款单,2预收款单")
    private Integer orderType;
    @Comment("单据日期")
    private LocalDate orderDate;

    @Comment("单据编号")
    private String orderNo;
    @Comment("单据来源,1进销存")
    private String documentSource;

    @Comment("折扣金额")
    private BigDecimal discountAmount;

    @Comment("收款金额")
    private BigDecimal collectionAmount;

    @Comment("总欠款")
    private BigDecimal totalAmountsOwed;


    @Comment("本次核销金额")
    private BigDecimal verificationAmount;
    @Comment("预收款金额")
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
    private String orderStatusName;

    @Comment("创建人")
    private Long createdBy;
    @Comment("最后修改人")
    private Long updateBy;
    @Comment("创建时间")
    private LocalDateTime createdAt;
    @Comment("最后修改时间")
    private LocalDateTime updateAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
