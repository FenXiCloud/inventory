package com.flyemu.share.entity.fund;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * @功能描述: 核销单
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */@JsonInclude()
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class Verification {
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
    private LocalDateTime updateAt;
    //最后修改人
    private Integer updateBy;
    private Integer approvedBy;
    private LocalDateTime approvedAt;
    //备注
    private String remarks;


}
