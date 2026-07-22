package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.service.basic.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/paymentMethod")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public JsonResult list(PaymentMethodService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(paymentMethodService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PaymentMethod paymentMethod, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(paymentMethod, accountDto);
        paymentMethod.setEnabled(true);
        paymentMethodService.save(paymentMethod);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PaymentMethod paymentMethod, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(paymentMethod, accountDto);
        paymentMethodService.save(paymentMethod);
        return JsonResult.successful();
    }

    @DeleteMapping("/{paymentMethodId}")
    public JsonResult delete(@PathVariable Long paymentMethodId, @SaAccountVal AccountDto accountDto) {
        paymentMethodService.delete(paymentMethodId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(paymentMethodService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
