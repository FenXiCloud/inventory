package com.flyemu.share.entity.setting;

import com.alibaba.fastjson2.JSONArray;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Comment("财务软件辅助映射")
public class FinanceItemMap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("辅助类型")
    private String categoryType;

    @Comment("财务软件辅助类型名称")
    private String categoryName;

    @Comment("财务软件辅助类型id")
    private Long categoryId;

    @Comment("财务软件映射辅助类型id")
    private Long financeId;

    @Comment("财务软件映射辅助类型名称")
    private String financeName;

    @Comment("进销存映射辅助类型id")
    private Long inventoryId;

    @Comment("进销存映射辅助类型名称")
    private String inventoryName;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("更新时间")
    private LocalDateTime updatedAt;
}
