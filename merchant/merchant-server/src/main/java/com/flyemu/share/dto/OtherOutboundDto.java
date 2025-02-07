package com.flyemu.share.dto;

import com.flyemu.share.entity.inventory.OtherOutbound;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtherOutboundDto extends OtherOutbound {

    private String customerCode;

    private String customerName;

    private Integer quantity;

    private String createdByName;
}
