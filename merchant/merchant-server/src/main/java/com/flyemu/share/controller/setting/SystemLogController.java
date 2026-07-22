package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.service.setting.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 操作日志 */
@RestController
@RequestMapping("/systemLog")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(Page page,
                           SystemLogService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(systemLogService.query(page, query));
    }

    @DeleteMapping("/{systemLogId}")
    public JsonResult delete(@PathVariable Long systemLogId, @SaAccountVal AccountDto accountDto) {
        systemLogService.delete(systemLogId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
