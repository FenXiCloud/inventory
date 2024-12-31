package com.flyemu.share.entity.basic;

import com.flyemu.share.dto.AuxiliaryUnitPrice;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.*;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @功能描述: 商品管理
 * @创建时间: 2024年04月28日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint( columnNames = {"merchantId", "accountBookId", "code"}),
})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("编码")
    @Column(length = 64, nullable = false)
    private String code;

    @Comment("名称")
    @Column(length = 64, nullable = false)
    private String name;

    @Comment("拼音")
    @Column(length = 32)
    private String pinyin;

    @Comment("条码")
    @Column(length = 32)
    private String barcode;

    @Comment("规格")
    private String specification;

    @Comment("备注")
    private String remarks;

    @Comment("预计进货价（基础单位）")
    @Column(nullable = false)
    private BigDecimal purchasePrice ;

    @Comment("排序")
    private Integer sort;

    @Comment("基础单位")
    @Column(nullable = false)
    private Long unitId;

    @Comment("是否启用辅助单位")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @Column(nullable = false)
    @ColumnDefault("b'0'")
    private Boolean enableAuxiliaryUnit;

    @Comment("辅助单位价格")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<AuxiliaryUnitPrice> auxiliaryUnitPrices;

    @Comment("商品图片")
    private String imgPath;

    @Comment("产品分类")
    @Column(nullable = false)
    private Long productCategoryId;

    @Comment("状态")
    @Column(nullable = false)
    @ColumnDefault("b'1'")
    private Boolean enabled;

    @Comment("库存数量")
    private Integer stockQuantity;

    @Comment("预警库存")
    private Integer alertQuantity;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
