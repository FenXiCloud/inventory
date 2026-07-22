package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.VerificationService;
import com.flyemu.share.form.VerificationForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping
    public JsonResult list(Page page, VerificationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(verificationService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(VerificationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(verificationService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid VerificationForm verification, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(verification.getOrder(), accountDto);
        verificationService.save(verification);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid VerificationForm verification, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(verification.getOrder(), accountDto);
        verificationService.save(verification);
        return JsonResult.successful();
    }

    @DeleteMapping("/{verificationId}")
    public JsonResult delete(@PathVariable Long verificationId, @SaAccountVal AccountDto accountDto) {
        verificationService.delete(String.valueOf(verificationId), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(verificationService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        verificationService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
