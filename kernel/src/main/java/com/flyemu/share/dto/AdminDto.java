package com.flyemu.share.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * @功能描述: AdminDto
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Setter
@Getter
public class AdminDto {

    private Integer id;

    private String name;

    private String mobile;

    private String username;

    private Boolean enabled;

    private Long merchantId;

    private Boolean systemDefault;

    private Long roleId;

    private Integer roleType;

    private String roleName;
}
