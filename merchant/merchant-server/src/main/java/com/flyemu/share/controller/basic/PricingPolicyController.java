package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.PricingPolicy;
import com.flyemu.share.form.price.PricingPolicyForm;
import com.flyemu.share.service.basic.PricingPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/pricingPolicy")
@RequiredArgsConstructor
public class PricingPolicyController {

    private final PricingPolicyService pricingPolicyService;

    @GetMapping
    public JsonResult list(PricingPolicyService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(pricingPolicyService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PricingPolicy pricingPolicy, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(pricingPolicy, accountDto);
        pricingPolicyService.save(pricingPolicy);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PricingPolicy pricingPolicy, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(pricingPolicy, accountDto);
        pricingPolicyService.save(pricingPolicy);
        return JsonResult.successful();
    }

    @DeleteMapping("/{pricingPolicyId}")
    public JsonResult delete(@PathVariable Long pricingPolicyId, @SaAccountVal AccountDto accountDto) {
        pricingPolicyService.delete(pricingPolicyId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(pricingPolicyService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PutMapping("/sort")
    public JsonResult sort(@RequestBody @Valid PricingPolicyForm pricingPolicyForm, @SaAccountVal AccountDto accountDto) {
        pricingPolicyService.sort(pricingPolicyForm, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

}
