package com.flyemu.share.dto;

import lombok.Data;

/**
 * @功能描述: MenuDto
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Data
public class MenuDto {

    private String title;
    private String key;
    private String icon;
    private String group;
    private Long id;
    private Long parentId;
}
