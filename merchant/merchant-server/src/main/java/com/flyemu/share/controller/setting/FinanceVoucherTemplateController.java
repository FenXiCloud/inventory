package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceVoucherTemplate;
import com.flyemu.share.service.setting.FinanceVoucherTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/financeVoucherTemplate")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceVoucherTemplateController {

    private final FinanceVoucherTemplateService financeVoucherTemplateService;

    @GetMapping
    public JsonResult list(FinanceVoucherTemplateService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(financeVoucherTemplateService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid FinanceVoucherTemplate financeVoucherTemplate, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(financeVoucherTemplate, accountDto);
        financeVoucherTemplateService.save(financeVoucherTemplate, accountDto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid FinanceVoucherTemplate financeVoucherTemplate, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(financeVoucherTemplate, accountDto);
        financeVoucherTemplateService.save(financeVoucherTemplate, accountDto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        financeVoucherTemplateService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeVoucherTemplateService.load(accountDto.getMerchantId(), id));
    }

}
