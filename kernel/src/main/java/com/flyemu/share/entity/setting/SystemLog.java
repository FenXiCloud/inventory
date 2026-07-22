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
public class SystemLog implements Serializable, TenantAware{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("操作模块 例如：用户管理、订单管理、商品管理等")
    private String module;

    @Comment("操作对象ID 例如：订单ID、商品ID、用户ID等")
    private Long objectId;

    @Comment("操作用户IP地址")
    private String ipAddress;

    @Comment("请求参数 例如：查询条件、提交的数据等")
    private String requestParams;

    @Comment("操作类型")
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OperationType operationType;

    @Comment("创建人")
    private Long createdBy;

    @Comment("创建时间")
    private LocalDateTime createdAt;

    @Comment("备注/描述")
    private String description;

    @Column(nullable = false)
    private Long accountBookId;

    @Column(nullable = false)
    private Long merchantId;

    public enum OperationType {
        登录, 新增, 修改, 删除, 查询, 导出, 导入
    }

}

