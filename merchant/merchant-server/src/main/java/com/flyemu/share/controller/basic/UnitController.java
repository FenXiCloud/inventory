package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Unit;
import com.flyemu.share.service.basic.UnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/unit")
@RequiredArgsConstructor
@SaCheckLogin
public class UnitController {

    private final UnitService unitService;

    @GetMapping
    public JsonResult list(UnitService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(unitService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Unit unit, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(unit, accountDto);
        unitService.save(unit);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Unit unit, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(unit, accountDto);
        unitService.save(unit);
        return JsonResult.successful();
    }

    @DeleteMapping("/{unitId}")
    public JsonResult delete(@PathVariable Long unitId, @SaAccountVal AccountDto accountDto) {
        unitService.delete(unitId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(unitService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
