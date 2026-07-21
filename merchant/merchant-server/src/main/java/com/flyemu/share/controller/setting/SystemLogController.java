package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.service.setting.SystemLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 操作日志
 */
@RestController
@RequestMapping("/systemLog")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(Page page,
                           SystemLogService.Query query,
                           @SaMerchantId Long merchantId,
                           @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(systemLogService.query(page, query));
    }

    @DeleteMapping("/{systemLogId}")
    public JsonResult delete(@PathVariable Long systemLogId,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId) {
        systemLogService.delete(systemLogId, merchantId, accountBookId);
        return JsonResult.successful();
    }
}
