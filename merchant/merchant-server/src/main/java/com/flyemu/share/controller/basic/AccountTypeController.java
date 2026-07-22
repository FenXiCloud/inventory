package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.AccountType;
import com.flyemu.share.service.basic.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/accountType")
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    public JsonResult list(AccountTypeService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(accountTypeService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountType accountType, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(accountType, accountDto);
        accountTypeService.save(accountType);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountType accountType, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(accountType, accountDto);
        accountTypeService.save(accountType);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountTypeId}")
    public JsonResult delete(@PathVariable Long accountTypeId, @SaAccountVal AccountDto accountDto) {
        accountTypeService.delete(accountTypeId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountTypeService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
