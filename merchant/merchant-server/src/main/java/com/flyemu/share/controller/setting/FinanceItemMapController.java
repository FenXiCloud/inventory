package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceItemMap;
import com.flyemu.share.service.setting.FinanceItemMapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/financeItemMap")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceItemMapController {

    private final FinanceItemMapService financeItemMapService;

    @GetMapping
    public JsonResult list(FinanceItemMapService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(financeItemMapService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid FinanceItemMap financeItemMap, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(financeItemMap, accountDto);
        financeItemMapService.save(financeItemMap, accountDto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid FinanceItemMap financeItemMap, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(financeItemMap, accountDto);
        financeItemMapService.save(financeItemMap, accountDto);
        return JsonResult.successful();
    }

    @PostMapping("/batch")
    public JsonResult batch(@RequestBody @Valid List<FinanceItemMap> financeItemMap, @SaAccountVal AccountDto accountDto) {
        for (FinanceItemMap itemMap : financeItemMap) {
            TenantScope.bind(itemMap, accountDto);
            financeItemMapService.save(itemMap, accountDto);
        }
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        financeItemMapService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeItemMapService.load(accountDto.getMerchantId(), id));
    }

}
