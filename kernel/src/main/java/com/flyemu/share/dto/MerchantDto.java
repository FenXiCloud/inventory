package com.flyemu.share.dto;

import lombok.Value;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @功能描述: MerchantDto
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Value
public class MerchantDto implements Serializable {

    Integer id;
    String code;
    String name;
    String address;
    LocalDateTime createDate;
    String linkman;
    String email;
    String phone;
    Boolean enabled;
    LocalDate checkDate;
    LocalDate serviceStartDate;
    LocalDate serviceEndDate;
    LocalDate startCheckDate;
}
