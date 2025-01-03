package com.flyemu.share.entity.fund;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;

/**
 * @功能描述: 其他支出单明细
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
public class OtherExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("其他支出单ID")
    private Long otherExpenseId;

    @Comment("收支类别ID")
    private Long accountTypeId;

    @Comment("金额")
    private BigDecimal amount;

    @Comment("备注")
    private String remarks;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
