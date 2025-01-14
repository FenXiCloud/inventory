package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @功能描述: 编码规则
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@DynamicInsert
public class CodeRule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("规则名称")
    @Column(length = 32, nullable = false)
    private String name;

    @Comment("前缀")
    private String prefix;

    @Comment("日期格式/分类编码")
    private String format;

    @Comment("流水号位数")
    private Integer serialNumberLength;

    @Comment("起始值")
    private Integer  startValue;

    @Comment("流水号清零")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ResetPeriod resetPeriod;

    @Comment("单据类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Comment("是否系统默认")
    @Column(nullable = false)
    @ColumnDefault("b'0'")
    private Boolean systemDefault;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum ResetPeriod {
        日, 月,季,年
    }

    public enum DocumentType {
        采购订单, 采购入库单, 采购退货单, 销售订单, 销售出库单, 销售退货单, 调拨单, 盘点单, 其他入库单, 其他出库单, 成本调整单, 收款单, 付款单, 核销单, 其他收款单, 其他付款单, 转帐单, 商品, 仓库, 客户, 供货商
    }

}

