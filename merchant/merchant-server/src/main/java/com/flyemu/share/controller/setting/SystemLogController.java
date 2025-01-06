package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.service.setting.SystemLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 打印模板
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/systemLog")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(SystemLogService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(systemLogService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SystemLog systemLog, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        systemLog.setMerchantId(merchantId);
        systemLog.setAccountBookId(accountBookId);
        systemLogService.save(systemLog);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SystemLog systemLog) {
        systemLogService.save(systemLog);
        return JsonResult.successful();
    }

    @DeleteMapping("/{systemLogId}")
    public JsonResult delete(@PathVariable Long systemLogId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        systemLogService.delete(systemLogId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(systemLogService.select(merchantId, accountBookId));
    }

}
