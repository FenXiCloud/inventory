package com.flyemu.share.entity.purchase;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "PurchaseInboundReturnConnection", indexes = {
        @Index(columnList = "purchaseReturnId"),
        @Index(columnList = "purchaseInboundId")
}, uniqueConstraints = {
        @UniqueConstraint(columnNames = {"purchaseReturnId", "purchaseInboundId"})
})
@DynamicUpdate
public class PurchaseInboundReturnConnection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("退货单ID")
    private Long purchaseReturnId;

    @Comment("采购入库主表ID")
    private Long purchaseInboundId;

    @Comment("创建时间")
    @CreationTimestamp
    private LocalDateTime createdAt;
}
