package com.flyemu.share.dto.invoice;

import lombok.Data;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * 通用分页返回结构（发票模块）。
 */
@Data
public class PagedResponse<T> {
    private List<T> content;
    private long total;
    private int page;
    private int size;

    public PagedResponse(List<T> content, long total, int page, int size) {
        this.content = content;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PagedResponse<T> of(List<T> content, long total, Pageable pageable) {
        return new PagedResponse<>(content, total, pageable.getPageNumber(), pageable.getPageSize());
    }
}
