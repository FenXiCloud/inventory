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
public class VerificationItem implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
    private Long verificationId;
    //创建时间
    private Date createTime;
    //更新时间
    private Date updateTime;
    //关联采购单ID
    private Integer businessId;
    //关联采购单编号
    private String businessNo;
    //业务类型
    private Integer businessType;
    //业务日期
    private Date businessDate;
    //业务源单据备注
    private String businessRemarks;
    //已核销金额
    private BigDecimal verifiedAmount;
    //本次核销金额
    private BigDecimal currentVerifyAmount;
    //单据金额
    private BigDecimal documentAmount;
    //未核销金额
    private BigDecimal unverifiedAmount;
    //备注
    private String remarks;

}
