package com.flyemu.share.form;

import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class InventoryInitialForm {

    private List<InventoryItem> inventoryItemList;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("创建人")
    private Long createdBy;
}
