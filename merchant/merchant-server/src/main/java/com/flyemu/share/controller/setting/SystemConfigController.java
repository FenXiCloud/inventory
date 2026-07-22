package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.SystemConfig;
import com.flyemu.share.service.setting.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/systemConfig")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public JsonResult list(SystemConfigService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(systemConfigService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SystemConfig systemConfig, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(systemConfig, accountDto);
        systemConfigService.save(systemConfig);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SystemConfig systemConfig, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(systemConfig, accountDto);
        systemConfigService.save(systemConfig);
        return JsonResult.successful();
    }

    @DeleteMapping("/{systemConfigId}")
    public JsonResult delete(@PathVariable Long systemConfigId, @SaAccountVal AccountDto accountDto) {
        systemConfigService.delete(systemConfigId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(systemConfigService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
