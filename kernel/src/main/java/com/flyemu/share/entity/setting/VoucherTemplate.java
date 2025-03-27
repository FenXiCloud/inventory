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

@Entity
@Getter
@Setter
@DynamicUpdate
@DynamicInsert
@Comment("凭证模板")
public class VoucherTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 64, nullable = false)
    @Comment("模板标题")
    private String title;

    @Comment("凭证字")
    private Long wordId;

    @Comment("模板明细")
    @JdbcTypeCode(SqlTypes.JSON)
    private JSONArray details;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
