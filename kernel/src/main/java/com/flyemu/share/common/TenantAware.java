package com.flyemu.share.common;

/**
 * 可绑定商户/账套租户字段的对象（Query、实体等）。
 */
public interface TenantAware {

    void setMerchantId(Long merchantId);

    void setAccountBookId(Long accountBookId);
}
