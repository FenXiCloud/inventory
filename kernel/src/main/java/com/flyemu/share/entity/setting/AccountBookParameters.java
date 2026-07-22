package com.flyemu.share.entity.setting;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicInsert;

import java.util.Date;
import java.io.Serializable;

/**
 * 账套参数设置(AccountBookParameters)实体类
 *
 * @author shuaiqi
 * @since 2025-05-13 15:35:35
 */
@Getter
@Setter
@Entity
@NoArgsConstructor
@Table
public class AccountBookParameters implements Serializable {
    /**
     * ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    /**
     * 账套id
     */
    private Integer accountBookId;
    /**
     * 成本核算方法 1.移动平均法 2.先进先出法
     */
    private Integer costAccounting;
    /**
     * 可用库存允许为负 1.是  2.否
     */
    private Integer availableInventory;
    /**
     * 数量小数位：0-8
     */
    private Integer quantityDecimal;
    /**
     * 小数位不能由大改小
     * 单价小数位：0-8
     */
    private Integer priceDecimal;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;

}

