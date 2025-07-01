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
 * @功能描述: 其他支出单
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
public class OtherExpense {

    @Comment("备注")
    private String remarks;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Comment("收支类别名称")
    private String settlementAccount;
    @Comment("收支类别ID")
    private Long settlementAccountId;
    @Comment("供应商ID")
    private Long supplierId;
    @Comment("供应商名称")
    private String supplierName;
    @Comment("单据日期")
    private LocalDate orderDate;
    @Comment("单据编号")
    private String orderNo;
    @Comment("欠款金额")
    private BigDecimal arrearsAmount;
    @Comment("付款金额")
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

    @Comment("业务员ID")
    private Long orderStaffId;
    @Comment("业务员名称")
    private String orderStaffName;
    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;
    @Comment("最后修改人")
    private Long updateBy;
    @Comment("最后修改时间")
    private LocalDateTime updateAt;
    @Column(nullable = false)
    private Long merchantId;
}
