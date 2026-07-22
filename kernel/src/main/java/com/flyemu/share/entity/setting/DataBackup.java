package com.flyemu.share.entity.setting;

import com.flyemu.share.common.TenantAware;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.io.Serializable;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
@DynamicUpdate
@DynamicInsert
public class DataBackup implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("备份文件名")
    @Column(length = 128, nullable = false)
    private String fileName;

    @Comment("相对存储路径")
    @Column(length = 255, nullable = false)
    private String filePath;

    @Comment("文件大小(字节)")
    private Long fileSize;

    @Comment("备注")
    @Column(length = 255)
    private String remarks;

    @Comment("操作人")
    private Long createdBy;

    @Comment("操作人名称")
    @Column(length = 64)
    private String createdByName;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;
}
