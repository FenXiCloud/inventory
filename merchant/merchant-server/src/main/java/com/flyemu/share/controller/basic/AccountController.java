package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Account;
import com.flyemu.share.service.basic.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public JsonResult list(AccountService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(accountService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Account account, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(account, accountDto);
        accountService.save(account);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Account account, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(account, accountDto);
        accountService.save(account);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountId}")
    public JsonResult delete(@PathVariable Long accountId, @SaAccountVal AccountDto accountDto) {
        accountService.delete(accountId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
