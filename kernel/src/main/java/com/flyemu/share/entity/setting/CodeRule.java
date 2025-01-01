package com.flyemu.share.entity.setting;

import com.flyemu.share.enums.DocumentType;
import com.flyemu.share.enums.ResetCycle;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;

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
public class CodeRule implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("规则名称")
    @Column(length = 32, nullable = false)
    private String name;

    @Comment("前缀")
    private String prefix;

    @Comment("日期格式")
    private String dateFormat;

    @Comment("流水号位数")
    private Integer serialNumberLength;

    @Comment("起始值")
    private Integer  startValue;

    @Comment("单据类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    @Comment("是否系统默认")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean systemDefault;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;


}

