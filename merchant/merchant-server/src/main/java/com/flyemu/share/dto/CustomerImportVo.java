package com.flyemu.share.dto;


import cn.hutool.core.annotation.Alias;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.math.BigDecimal;


@Data
public class CustomerImportVo {

    @Comment("客户编码")
    @Alias("客户编码")
    private String code;

    @Comment("客户名称")
    @Alias("客户名称")
    private String name;

    @Comment("联系人")
    @Alias("联系人")
    private String contact;

    @Comment("电话")
    @Alias("电话")
    private String phone;

    @Comment("备注")
    private String remarks;

    @Comment("客户分类ID")
    private Long customerCategoryId;

    @Comment("客户分类")
    @Alias("客户分类")
    private String customerCategoryName;

    @Comment("客户等级ID")
    private Long customerLevelId;

    @Comment("客户等级")
    @Alias("客户等级")
    private String customerLevelName;

    @Comment("客户余额,应收账款")
    private BigDecimal balance;

    @Comment("状态")
    private Boolean enabled;

    private Long accountBookId;

    private Long merchantId;

}
