package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.PricingPolicy;
import com.flyemu.share.service.basic.PricingPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 价格策略
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/pricingPolicy")
@RequiredArgsConstructor
public class PricingPolicyController {

    private final PricingPolicyService pricingPolicyService;

    @GetMapping
    public JsonResult list(PricingPolicyService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(pricingPolicyService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PricingPolicy pricingPolicy, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        pricingPolicy.setMerchantId(merchantId);
        pricingPolicy.setAccountBookId(accountBookId);
        pricingPolicyService.save(pricingPolicy);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PricingPolicy pricingPolicy) {
        pricingPolicyService.save(pricingPolicy);
        return JsonResult.successful();
    }

    @DeleteMapping("/{pricingPolicyId}")
    public JsonResult delete(@PathVariable Long pricingPolicyId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        pricingPolicyService.delete(pricingPolicyId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(pricingPolicyService.select(merchantId, accountBookId));
    }

}
