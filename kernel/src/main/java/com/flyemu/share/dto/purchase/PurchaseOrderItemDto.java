package com.flyemu.share.dto.purchase;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @功能描述: 采购订单明细Dto
 * @创建时间: 2023年02月07日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class PurchaseOrderItemDto {

    private Long id;

    /**
     * 采购订单主表ID
     */
    private Long purchaseOrderId;

    /**
     * 产品ID
     */
    private Long productId;

    /**
     * 产品编码
     */
    private String productCode;

    /**
     * 产品名称
     */
    private String productName;

    /**
     * 基本单位ID
     */
    private Long baseUnitId;

    /**
     * 基本单位名称
     */
    private String baseUnitName;

    /**
     * 基本数量
     */
    private Double quantity;

    /**
     * 采购单位ID
     */
    private Long secondaryUnitId;

    /**
     * 采购单位名称
     */
    private String secondaryUnitName;

    /**
     * 采购数量
     */
    private Double secondaryQuantity;

    /**
     * 采购单价
     */
    private Double secondaryPrice;

    /**
     * 换算率
     */
    private BigDecimal conversionRate;

    /**
     * 基本单价
     */
    private BigDecimal unitPrice;

    /**
     * 折扣率
     */
    private BigDecimal discountRate;

    /**
     * 折扣金额
     */
    private BigDecimal discountAmount;

    /**
     * 小计
     */
    private BigDecimal subtotal;

    /**
     * 仓库ID
     */
    private Long warehouseId;

    /**
     * 仓库Name
     */
    private String warehouseName;

    /**
     * 创建人
     */
    private Long createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    /**
     * 备注
     */
    private String remark;

}
