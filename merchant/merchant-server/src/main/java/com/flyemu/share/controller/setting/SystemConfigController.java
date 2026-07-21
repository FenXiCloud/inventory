package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.SystemConfig;
import com.flyemu.share.service.setting.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 系统参数
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/systemConfig")
@RequiredArgsConstructor
public class SystemConfigController {

    private final SystemConfigService systemConfigService;

    @GetMapping
    public JsonResult list(SystemConfigService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(systemConfigService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SystemConfig systemConfig, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        systemConfig.setMerchantId(merchantId);
        systemConfig.setAccountBookId(accountBookId);
        systemConfigService.save(systemConfig);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SystemConfig systemConfig, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        systemConfig.setMerchantId(merchantId);
        systemConfig.setAccountBookId(accountBookId);
        systemConfigService.save(systemConfig);
        return JsonResult.successful();
    }

    @DeleteMapping("/{systemConfigId}")
    public JsonResult delete(@PathVariable Long systemConfigId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        systemConfigService.delete(systemConfigId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(systemConfigService.select(merchantId, accountBookId));
    }

}
