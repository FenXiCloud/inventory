package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
public class Settlement implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    /** 1=客户结算（关联收款单） 2=供应商结算（关联付款单） */
    private Integer type;

    /** 客户/供应商ID */
    private Long personnelId;

    /** 客户/供应商名称 */
    private String personnelName;

    /** 单据日期 */
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDateTime orderDate;

    /** 单据编号 */
    private String orderNo;

    /** 状态 */
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    /** 业务员ID */
    private Integer orderStaffId;

    /** 业务员名称 */
    private String orderStaffName;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 创建人 */
    private Integer createdBy;

    /** 最后修改时间 */
    @Column(name = "update_at")
    private LocalDateTime updatedAt;

    /** 最后修改人 */
    @Column(name = "update_by")
    private Integer updatedBy;

    private Integer approvedBy;

    private LocalDateTime approvedAt;

    /** 备注 */
    private String remarks;

    // ===== 以下为列表核对展示用临时字段(@Transient 不落库,仅 SettlementService.query 填充) =====

    /** 单据金额(INVENTORY 明细合计) */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal sumDocumentAmount;

    /** 已核销/实收金额(INVENTORY 明细合计) */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal sumVerifiedAmount;

    /** 剩余未结(INVENTORY 明细合计) */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private BigDecimal sumUnverifiedAmount;

    /** 源单据号(多条用、连接) */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sourceBusinessNo;

    /** 源单据类型(销售出库单/采购入库单) */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String sourceBusinessType;

    /** 源单据日期 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate sourceDate;

    /** 状态异常：已平账却仍有剩余(>0.005),用于列表红标提示核对 */
    @Transient
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean statusMismatch;
}
