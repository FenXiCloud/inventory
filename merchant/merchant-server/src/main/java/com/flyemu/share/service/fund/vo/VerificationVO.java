package com.flyemu.share.service.fund.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

/**
 *@author shuaiqi
 */@JsonInclude()
@Data
public class VerificationVO {

    private String createName;
    private String updateName;
    private String approvedName;
    private Long id;

    private Long accountBookId;

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
