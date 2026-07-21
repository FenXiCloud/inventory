package com.flyemu.share.entity.fund;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @功能描述: 其他收入单明细
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@JsonInclude()
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "other_income_item")
@DynamicUpdate
public class OtherReceiptItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("其他收入单ID")
    @Column(name = "other_income_id")
    private Long otherReceiptId;

    @Comment("收支类别ID")
    private Long accountTypeId;
    @Comment("收支类别名称")
    private String accountTypeName;

    @Comment("收入金额")
    private BigDecimal amount;

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
    @Comment("源单号")
    private String sourceDocNo;
    @Comment("源单往来单位")
    private String sourceBusinessName;
    @Comment("源单日期")
    private LocalDate sourceDate;
}
