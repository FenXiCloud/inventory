package com.flyemu.share.dto.price;

import com.flyemu.share.enums.PriceSource;
import com.flyemu.share.enums.PriceType;
import lombok.Data;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;

import java.io.Serializable;

@Data
public class PricingPolicyDto implements Serializable {

    private Long id;

    @Comment("优先级")
    private Integer priority;

    @Comment("价格类型")
    private PriceType priceType;

    @Comment("价格来源")
    private PriceSource priceSource;

    @Comment("状态")
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("备注")
    private String remarks;

    private Long accountBookId;

    private Long merchantId;

}

