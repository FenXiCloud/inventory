package com.flyemu.share.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 *@author shuaiqi
 */
@JsonInclude()
@Data
public class OtherFundDetailsVO {

    // 日期
    private LocalDate date;
    // 单据编号
    private String documentNumber;
    // 业务员
    private String staffName;
    // 收支类别
    private String accountType;
    // 收支项目 (收入/支出)
    private String fundType;
    // 收入
    private BigDecimal amount;
    // 往来单位
    private String businessPartner;

}