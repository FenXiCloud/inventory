package com.flyemu.share.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flyemu.share.common.Constants;
import lombok.Getter;
import lombok.Setter;

/**
 * @功能描述: 分页
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
public class Page {

    private int pageSize = Constants.PAGE_SIZE;

    @JsonIgnore
    private int offset;

    private int size;

    @JsonIgnore
    private int offsetEnd;

    private int page = 1;

    private String property, order;

    public int getOffset() {
        return (page - 1) * pageSize;
    }

    public int getOffsetEnd() {
        return pageSize;
    }

    public Page setSize(int size) {
        this.size = size;
        this.pageSize = size;
        return this;
    }

    public Page setPageSize(int pageSize) {
        this.pageSize = pageSize;
        this.size = pageSize;
        return this;
    }
}
