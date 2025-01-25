package com.flyemu.share.form;

import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOrderItem;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.entity.sales.SalesOutboundItem;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Data
public class SalesOutboundForm {

    private SalesOutbound salesOutbound;

    private List<SalesOutboundItem> salesOutboundItemList;

    private List<Long> orderIds;
}
