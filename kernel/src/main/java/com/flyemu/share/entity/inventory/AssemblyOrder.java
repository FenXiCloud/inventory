package com.flyemu.share.entity.inventory;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.enums.AssemblyOrderType;
import com.flyemu.share.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class AssemblyOrder implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("单据编号")
    private String orderNo;

    @Comment("单据类型：组装/拆卸")
    @Column(nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private AssemblyOrderType orderType;

    @Comment("成品(组装)/被拆品(拆卸)商品ID")
    private Long productId;

    @Comment("数量")
    @Column(precision = 18, scale = 4)
    private BigDecimal quantity;

    @Comment("仓库ID")
    private Long warehouseId;

    @Comment("备注")
    private String remarks;

    @Comment("主品成本金额（审核时写入，反审核回补用）")
    private BigDecimal costAmount;

    @Comment("订单状态")
    @Column(nullable = false, length = 32, columnDefinition = "varchar(20) default '已保存'")
    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("审核人")
    private Long approvedBy;

    @Comment("审核时间")
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
