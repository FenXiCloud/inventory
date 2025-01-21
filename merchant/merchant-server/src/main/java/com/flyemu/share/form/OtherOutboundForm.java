package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.entity.inventory.OtherOutboundItem;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class OtherOutboundForm implements Serializable {

    private OtherOutbound otherOutbound;

    private List<OtherOutboundItem> otherOutboundItems;
}
