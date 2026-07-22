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

import java.time.LocalDateTime;

@JsonInclude()
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class Verification implements TenantAware {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
    //1预收冲应收 2预付冲应付
    private Integer type;
    //人员id
    private Long personnelId;
    //人员名称
    private String personnelName;
    //单据日期
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDateTime orderDate;
    //单据编号
    private String orderNo;
    //状态
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;
    //业务员ID
    private Integer orderStaffId;
    //业务员名称
    private String orderStaffName;
    //创建时间
    private LocalDateTime createdAt;
    //创建人
    private Integer createdBy;
    //最后修改时间
    @Column(name = "update_at")
    private LocalDateTime updatedAt;
    //最后修改人
    @Column(name = "update_by")
    private Integer updatedBy;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    //备注
    private String remarks;

}
