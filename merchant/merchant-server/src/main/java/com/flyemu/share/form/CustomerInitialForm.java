package com.flyemu.share.form;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.entity.fund.CustomerFlow;
import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.util.List;

@Data
public class CustomerInitialForm implements TenantAware {

    private List<CustomerFlow> customerFlowList;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("创建人")
    private Long createdBy;

    private List<Long> ids;
}
