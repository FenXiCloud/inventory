package com.flyemu.share.controller;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flyemu.share.common.Constants;
import lombok.Getter;
import lombok.Setter;

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
