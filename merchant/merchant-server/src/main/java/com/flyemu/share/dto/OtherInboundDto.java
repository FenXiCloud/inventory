package com.flyemu.share.dto;

import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.entity.inventory.OtherOutbound;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OtherInboundDto extends OtherInbound {

    private String customerCode;

    private String customerName;

    private Integer quantity;

    private String createdByName;

    private String supplierCode;

    private String supplierName;
}
