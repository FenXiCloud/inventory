package com.flyemu.share.entity.purchase;

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
 * @功能描述: 采购退单
 * @创建时间: 2024年12月31日
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
public class PurchaseReturn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("采购入库主表ID")
    private Long purchaseInboundId;

    @Comment("供货商ID")
    private Long supplierId;

    @Comment("退单日期")
    private LocalDate returnDate;

    @Comment("退款金额")
    private BigDecimal refundAmount;

    @Comment("退单原因")
    private String returnReason;

    @Comment("备注")
    private String remarks;

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
