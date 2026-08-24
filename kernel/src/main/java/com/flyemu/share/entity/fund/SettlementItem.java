package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.util.Date;

@JsonInclude()
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class SettlementItem implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    /** 结算单ID */
    private Long settlementId;

    /** 关联单据ID */
    private Long businessId;

    /** 关联单据编号 */
    private String businessNo;

    /** 单据类别：INVENTORY(出入库单) / FUND(收付款单) */
    private String businessCategory;

    /** 业务类型：收款单/付款单/采购入库单/销售出库单等 */
    private String businessType;

    /** 业务日期 */
    private Date businessDate;

    /** 业务源单据备注 */
    private String businessRemarks;

    /** 单据金额 */
    private BigDecimal documentAmount;

    /** 已结算金额 */
    private BigDecimal verifiedAmount;

    /** 本次结算金额 */
    private BigDecimal currentVerifyAmount;

    /** 未结算金额 */
    private BigDecimal unverifiedAmount;

    /** 备注 */
    private String remarks;

    /** 创建时间 */
    private Date createTime;

    /** 更新时间 */
    private Date updateTime;
}
