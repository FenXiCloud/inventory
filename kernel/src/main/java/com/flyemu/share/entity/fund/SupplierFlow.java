package com.flyemu.share.entity.fund;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @功能描述: 供货商应付，预付流水
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
@JsonInclude ()
public class SupplierFlow implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Comment("货商Id")
    @Column(nullable = false)
    private Long supplierId;
    @Comment("单据Id")
    private Long businessId;
    @Comment("单据编号")
    private String businessNo;
    @Comment("单据日期")
    private LocalDate businessDate;
    @Comment("操作类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private SupplierFlowType supplierFlowType;
    @Comment("采购金额")
    private BigDecimal purchaseAmount;
    @Comment("优惠金额")
    private BigDecimal preferentialAmount;
    @Comment("应付金额")
    private BigDecimal copeWithAmount;
    @Comment("实付金额")
    private BigDecimal actualPaymentAmount;
    @Comment("应付余额")
    private BigDecimal balancePayable;
    @Comment("创建人")
    private Long createdBy;
    @Comment("创建时间")
    private LocalDateTime createdAt;
    @Comment("备注")
    private String remarks;
    @Column(nullable = false)
    private Long accountBookId;
    @Column(nullable = false)
    private Long merchantId;

    public enum SupplierFlowType {
        期初,
        采购入库单,
        反审核_采购入库单,

        采购退货单,
        反审核_采购退货单,

        付款单,
        反审核_付款单,
        其他支出单,
        反审核_其他支出单;
    }

}

