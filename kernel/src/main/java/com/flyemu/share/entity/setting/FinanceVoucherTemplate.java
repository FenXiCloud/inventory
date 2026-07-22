package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import com.alibaba.fastjson.JSONArray;
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
@Comment("财务软件凭证模板")
public class FinanceVoucherTemplate implements TenantAware {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    @Comment("模板标题")
    private String title;

    @Comment("模板类型")
    private String type;

    @Comment("凭证字id")
    private Long wordId;

    @Comment("凭证字")
    private String word;

    @Comment("模板明细")
    @JdbcTypeCode(SqlTypes.JSON)
    private JSONArray details;

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
