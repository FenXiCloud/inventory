package com.flyemu.share.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
public class AccountBookDto {

    private Long id;

    private String name;

    private LocalDate checkoutDate;

    private Boolean current;

    private Boolean enabled;

    private LocalDate startDate;

    private Integer warehouseId;

    private String warehouseName;

    private Long merchantId;

    /** 成本核算方法 1.移动平均法 2.先进先出法（账套参数，未设置时为 null，前端按默认值展示） */
    private Integer costAccounting;

    /** 可用库存允许为负 1.是 2.否 */
    private Integer availableInventory;

    /** 数量小数位：0-8 */
    private Integer quantityDecimal;

    /** 单价小数位：0-8 */
    private Integer priceDecimal;
}
