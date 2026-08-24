package com.flyemu.share.form;

import lombok.Data;

/**
 * 以销定购看板 - 列表查询表单
 */
@Data
public class ToOrderListQuery {

    /** 状态筛选：0待处理 / 1部分采购 / 2已采购 */
    private Integer pendingStatus;

    /** 开始日期 yyyy-MM-dd */
    private String startDate;

    /** 结束日期 yyyy-MM-dd */
    private String endDate;

    /** 客户名称（模糊） */
    private String customerName;

    /** 订单编号（模糊） */
    private String orderNo;

    /** 页码 */
    private Integer pageNum;

    /** 每页条数 */
    private Integer pageSize;
}
