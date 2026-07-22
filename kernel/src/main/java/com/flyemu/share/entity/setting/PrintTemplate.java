package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import com.alibaba.fastjson.JSONArray;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@DynamicInsert
public class PrintTemplate implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("名称")
    @Column(length = 32, nullable = false)
    private String name;

    @Comment("单据类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Comment("是否系统默认")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean systemDefault;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("模板内容JSON")
    @JdbcTypeCode(SqlTypes.JSON)
    private JSONArray content;

    public enum DocumentType {
        采购订单, 采购入库单, 采购退货单, 销售订单, 销售出库单, 销售退货单, 调拨单, 盘点单, 其他入库单, 其他出库单, 成本调整单, 收款单, 付款单, 核销单, 其他收款单, 其他付款单, 转帐单
    }

}

