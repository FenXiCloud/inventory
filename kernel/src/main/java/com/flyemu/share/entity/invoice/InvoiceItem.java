package com.flyemu.share.entity.invoice;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * 发票明细行。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class InvoiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @Comment("行号")
    private Integer lineNo;

    @Comment("商品/服务名称")
    private String goodsName;

    @Comment("商品税收分类编码")
    private String goodsCode;

    @Comment("商品和服务分类简称 (spmc)")
    private String goodsShortName;

    @Comment("规格型号")
    private String model;

    @Comment("单位")
    private String unit;

    @Comment("数量")
    private BigDecimal quantity;

    @Comment("单价（含税）")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal unitPrice;

    @Comment("金额（不含税）")
    private BigDecimal amount;

    @Comment("税率")
    private BigDecimal taxRate;

    @Comment("税额")
    private BigDecimal tax;

    @Comment("账套ID")
    private Long accountBookId;

    @Comment("商户ID")
    @Column(nullable = false)
    private Long merchantId;
}
