package com.flyemu.share.service.fund.vo.report;

import com.flyemu.share.common.TenantAware;
import lombok.Data;

import java.time.LocalDate;

@Data
public class SummaryReceivableDetailsQuery implements TenantAware {
    private Long merchantId;
    private Long accountBookId;
    private Long customerId;
    private Long customerTypeId;
    private Integer  salesmanId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer type; // 1=按客户，2=按客户类型，3=按业务员
}
