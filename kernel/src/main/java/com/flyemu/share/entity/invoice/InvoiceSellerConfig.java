package com.flyemu.share.entity.invoice;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

/**
 * 开票方（销方）税号配置 — 每个商户一条。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class InvoiceSellerConfig implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("纳税人识别号（税号）")
    @Column(length = 64)
    private String nsrsbh;

    @Comment("销方名称")
    @Column(length = 200)
    private String name;

    @Comment("销方电话")
    @Column(length = 32)
    private String phone;

    @Comment("销方地址")
    @Column(length = 300)
    private String address;

    @Comment("开户银行")
    @Column(length = 200)
    private String bank;

    @Comment("银行账号")
    @Column(length = 64)
    private String bankAccount;

    @Comment("发票类型: 030=数电普票, 032=数电专票")
    @Column(length = 8)
    private String invoiceType = "030";

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
