package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.entity.inventory.OtherInboundItem;
import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.entity.inventory.OtherOutboundItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OtherInboundForm implements Serializable {

    private OtherInbound otherInbound;

    private List<OtherInboundItem> otherInboundItems;
}
