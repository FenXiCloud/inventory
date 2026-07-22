package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.CustomerLevel;
import com.flyemu.share.service.basic.CustomerLevelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customerLevel")
@RequiredArgsConstructor
@SaCheckLogin
public class CustomerLevelController {

    private final CustomerLevelService customerLevelService;

    @GetMapping
    public JsonResult list(CustomerLevelService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(customerLevelService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerLevel customerLevel, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerLevel, accountDto);
        customerLevelService.save(customerLevel);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CustomerLevel customerLevel, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerLevel, accountDto);
        customerLevelService.save(customerLevel);
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerLevelId}")
    public JsonResult delete(@PathVariable Long customerLevelId, @SaAccountVal AccountDto accountDto) {
        customerLevelService.delete(customerLevelId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(customerLevelService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
