package com.flyemu.share.entity.basic;

import com.flyemu.share.common.TenantAware;
import com.flyemu.share.dto.AuxiliaryUnitPrice;
import com.flyemu.share.dto.ProductAttributeValue;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@DynamicUpdate
@Table(uniqueConstraints = {
        @UniqueConstraint( columnNames = {"merchantId", "accountBookId", "code"}),
})
public class Product implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("编码")
    @Column(length = 64, nullable = false)
    private String code;

    @Comment("名称")
    @Column(length = 255, nullable = false)
    private String name;

    @Comment("拼音")
    @Column(length = 32)
    private String pinyin;

    @Comment("条码")
    @Column(length = 32)
    private String barcode;

    @Comment("规格")
    private String specification;

    @Comment("品牌")
    @Column(length = 64)
    private String brand;

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
    private Boolean enableMultiUnit;

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

    @Comment("库存上限")
    private Integer maxStockQuantity;

    @Comment("整件数量（1整件=多少基本单位，如：1箱=20斤）")
    private Integer caseQuantity;

    @Comment("整件单位ID")
    private Long caseUnitId;

    @Comment("默认整件货位ID")
    private Long defaultWholeLocationId;

    @Comment("默认零货货位ID")
    private Long defaultZeroLocationId;

    @Comment("启用批次/保质期管理")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @ColumnDefault("b'0'")
    private Boolean enableBatch;

    @Comment("启用序列号管理")
    @JdbcTypeCode(SqlTypes.BOOLEAN)
    @ColumnDefault("b'0'")
    private Boolean enableSerial;

    @Comment("最高采购价")
    private BigDecimal maxPurchasePrice ;

    @Comment("零售客户价")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal retailCustomerPrice;
    @Comment("批发客户价")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal wholesaleCustomerPrice;
    @Comment("VIP客户价")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal vipCustomerPrice;

    @Comment("最低销售价")
    @Column(precision = 38, scale = 6) // 单价族口径：小数位上限 6（见 sql/price_scale_widen.sql）
    private BigDecimal minSalesPrice;

    @Comment("税率")
    private BigDecimal taxRate;

    @Comment("是否可采购（以销定购筛选）")
    @Column(nullable = false)
    private Boolean purchasable = true;

    @Comment("默认供应商ID")
    private Long defaultSupplierId;

    @Comment("税收分类编码")
    private String goodsCode;

    @Comment("辅助属性")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<ProductAttributeValue> productAttributes;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

}
