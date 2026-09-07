package com.flyemu.share.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户档案附件（证件附件 / 资格证附件）
 * 仅存元数据，文件本体在 /attachment/customer/ 下由 UploadController 托管。
 */
@Data
@NoArgsConstructor
public class CustomerAttachment {

    /** 分类：certificate=证件附件，qualification=资格证附件 */
    private String category;

    /** 展示名称（默认取上传时的原始文件名） */
    private String name;

    /** 文件访问路径，形如 /attachment/customer/{uuid}.{ext} */
    private String filePath;

    /** 文件大小（字节） */
    private Long fileSize;

    public CustomerAttachment(String category, String name, String filePath, Long fileSize) {
        this.category = category;
        this.name = name;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }
}
