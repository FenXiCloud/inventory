package com.flyemu.share.entity.fund;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class OtherExpenseItem implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("其他支出单ID")
    private Long otherExpenseId;

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
