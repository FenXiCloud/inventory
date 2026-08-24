package com.flyemu.share.entity.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 进项发票（供应商开给本商户的采购发票），用于进项归集。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class InputInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("供应商名称")
    @Column(length = 200)
    private String supplierName;

    @Comment("供应商税号")
    @Column(length = 64)
    private String supplierTaxNo;

    @Comment("发票代码")
    @Column(length = 64)
    private String invoiceCode;

    @Comment("发票号码")
    @Column(length = 64)
    private String invoiceNo;

    @Comment("开票日期")
    private LocalDate issueDate;

    @Comment("金额（不含税）")
    private BigDecimal amount;

    @Comment("税额")
    private BigDecimal tax;

    @Comment("价税合计")
    private BigDecimal totalAmount;

    @Comment("备注")
    private String remark;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("账套ID")
    private Long accountBookId;

    @Comment("商户ID")
    @Column(nullable = false)
    private Long merchantId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
