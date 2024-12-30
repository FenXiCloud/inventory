package com.flyemu.share.controller;

import com.blazebit.persistence.PagedList;
import lombok.Getter;
import lombok.Setter;

import java.util.Collection;

/**
 * @功能描述: 分页
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
public class PageResults<T> extends Page {
    private int totalPage;

    private long total;

    private Collection<T> results;

    private Object data;

    public PageResults(int page, Collection<T> results, long total, int pageSize) {
        this.setPage(page);
        this.results = results;
        this.setTotal(total);
        this.setPageSize(pageSize);
        this.setTotalPage((int) Math.ceil(total / (double) pageSize));
    }

    public PageResults(Collection<T> results, Page page, long total) {
        this.setPage(page.getPage());
        this.results = results;
        this.setTotal(total);
        this.setPageSize(page.getPageSize());
        this.setTotalPage((int) Math.ceil(total / (double) page.getPageSize()));
    }

    public PageResults(PagedList<T> pagedList, Page page) {
        this.results = pagedList;
        this.setPage(page.getPage());
        this.setTotal(pagedList.getTotalSize());
        this.setTotalPage(pagedList.getTotalPages());
        this.setPageSize(page.getPageSize());
    }

    public PageResults setData(Object data) {
        this.data = data;
        return this;
    }

    public PageResults setResults(Collection<T> results) {
        this.results = results;
        return this;
    }

    public PageResults<T> setResults(Collection<T> results, long total) {
        this.results = results;
        this.setTotal(total);
        this.setTotalPage((int) Math.ceil(total / (double) this.getPageSize()));
        return this;
    }

    public boolean isFirst() {
        return this.getPage() == 0;
    }

    /**
     * @return 是否最后一页
     */
    public boolean isLast() {
        return this.getPage() >= (this.getTotalPage());
    }
}
