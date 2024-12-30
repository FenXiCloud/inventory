package com.flyemu.share.entity.setting;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @功能描述: 商户实体类
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@ToString
public class Merchant implements Serializable {
    private static final long serialVersionUID = 952729070027371070L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Comment("商户编码")
    private String code;

    @Comment("商户名称")
    private String name;

    @Comment("地址")
    private String address;

    @Comment("创建日期")
    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createDate;

    @Comment("联系人")
    private String linkman;

    @Comment("邮箱")
    private String email;

    @Comment("电话号码")
    private String mobile;

    @Comment("是否启用")
    private Boolean enabled;

    @Comment("服务开始时间")
    private LocalDate serviceStartDate;

    @Comment("服务结束时间")
    private LocalDate serviceEndDate;

    @Comment("存货计价方式")
    private CostingMethod costingMethod;

    @Comment("允许负库存")
    private Boolean stockField;

    public enum CostingMethod {
        移动平均法,先进先出法
    }

}

