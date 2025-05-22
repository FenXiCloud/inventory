package com.flyemu.share.service.fund.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @author shuaiqi
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VerificationInfo {

    private BigDecimal documentAmount;     // 销售单总金额
    private BigDecimal verifiedAmount;      // 累计已核销金额
    private BigDecimal unverifiedAmount;    // 未核销金额


}
