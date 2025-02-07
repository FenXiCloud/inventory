package com.flyemu.share.dto;


import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;


@Data
public class CustomerImportVo {

    @Comment("编码")
    private String code;

    @Comment("名称")
    private String name;

    @Comment("联系人")
    private String contact;

    @Comment("电话")
    private String phone;

    @Comment("备注")
    private String remarks;

    @Comment("客户分类ID")
    private Long customerCategoryId;

    @Comment("客户分类名称")
    private String customerCategoryName;

    @Comment("客户等级ID")
    private Long customerLevelId;

    @Comment("客户等级名称")
    private Long customerLevelName;

    @Comment("客户余额,应收账款")
    private BigDecimal balance;

    @Comment("状态")
    private Boolean enabled;

    private Long accountBookId;

    private Long merchantId;

}
