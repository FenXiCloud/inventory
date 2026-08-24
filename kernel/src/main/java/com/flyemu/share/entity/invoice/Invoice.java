package com.flyemu.share.entity.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 数电发票主表（蓝字/红字）。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("发票类型: BLUE=蓝字发票, RED=红字发票")
    @Column(length = 8)
    private String invoiceType;

    @Comment("本地发票号")
    @Column(length = 64)
    private String invoiceNo;

    @Comment("红字发票对应的原蓝字发票号码")
    @Column(length = 64)
    private String originalInvoiceNo;

    @Comment("红字发票对应的原蓝字开票日期")
    @Column(length = 32)
    private String originalInvoiceDate;

    @Comment("购买方名称")
    @Column(length = 200)
    private String buyerName;

    @Comment("购买方税号")
    @Column(length = 64)
    private String buyerTaxNo;

    @Comment("价税合计")
    private BigDecimal totalAmount;

    @Comment("合计税额")
    private BigDecimal totalTax;

    @Comment("状态: PROCESSING / ISSUED / FAILED")
    @Column(length = 32)
    private String status;

    @Comment("开票日期")
    private LocalDateTime issueDate;

    @Comment("第三方发票代码")
    @Column(length = 64)
    private String thirdPartyCode;

    @Comment("第三方发票号码")
    @Column(length = 64)
    private String thirdPartyNumber;

    @Comment("PDF 存储路径")
    @Column(length = 500)
    private String pdfUrl;

    @Comment("开票人")
    @Column(length = 100)
    private String issuer;

    @Comment("发票明细")
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<InvoiceItem> items;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Comment("账套ID")
    private Long accountBookId;

    @Comment("商户ID")
    @Column(nullable = false)
    private Long merchantId;

    @Comment("来源单据ID（如销售出库单ID）")
    private Long sourceId;

    @Comment("来源单据类型: SALES_OUTBOUND")
    @Column(length = 32)
    private String sourceType;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
