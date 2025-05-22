package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.Comment;

import java.io.Serializable;

/**
 * @author: wangwenjia
 * @since: 2025/5/20 17:35
 * @description:
 */
@Data
@Entity
@Table
public class Dept  implements Serializable {
    private static final long serialVersionUID = 224044689322574706L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("部门名称")
    private String deptName;

    @Comment("父级id")
    private Long parentId;

    /**
     *  0未删除 1已删除
     */
    @Comment("是否删除")
    private String delFlag;

    /**
     *  0启用 1停用
     */
    @Comment("状态")
    private String status;

    private String ancestors;


}
