package com.flyemu.share.entity.invoice;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;

/**
 * 开票项目 — 商品/服务税收分类编码。
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
public class GoodsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("开票项目名称，如 *技术服务*信息技术服务费")
    @Column(length = 300)
    private String goodsName;

    @Comment("税收分类编码，19位")
    @Column(length = 32)
    private String goodsCode;

    @Comment("规格型号")
    @Column(length = 200)
    private String spec;

    @Comment("计量单位")
    @Column(length = 50)
    private String unit;

    @Comment("税率标签，如 3%、1%、6%、13%、免税")
    @Column(length = 20)
    private String taxRateLabel;

    @Comment("是否默认项目")
    private boolean isDefault;

    @Comment("是否享受税收优惠")
    private boolean taxPreference;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;

    @Comment("账套ID")
    private Long accountBookId;

    @Comment("商户ID")
    @Column(nullable = false)
    private Long merchantId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
