package com.flyemu.share.dto;

import com.flyemu.share.entity.inventory.StockTake;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StockTakeDto extends StockTake {

    private String warehouseName;

    private String createdByName;

    private List<String> orderNos;

    /** 关联盘盈/盘亏单据（可点击打开） */
    private List<RelatedOrder> relatedOrders;

    /** 是否存在盘盈明细（实盘 > 账面） */
    private Boolean needInbound;

    /** 是否存在盘亏明细（实盘 < 账面） */
    private Boolean needOutbound;

    /** 是否已生成盘盈其他入库单 */
    private Boolean inboundGenerated;

    /** 是否已生成盘亏其他出库单 */
    private Boolean outboundGenerated;

    @Data
    public static class RelatedOrder {
        private Long id;
        private String orderNo;
        /** inbound | outbound */
        private String type;
        private String label;
    }
}
