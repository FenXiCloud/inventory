package com.flyemu.share.controller.setting;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.PrintTemplate;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.service.setting.PrintTemplateService;
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
@RequestMapping("/printTemplate")
@RequiredArgsConstructor
public class PrintTemplateController {

    private final PrintTemplateService printTemplateService;
    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(PrintTemplateService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(printTemplateService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PrintTemplate printTemplate,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId,
                           @SaAdminId Long adminId) {
        printTemplate.setMerchantId(merchantId);
        printTemplate.setAccountBookId(accountBookId);
        PrintTemplate saved = printTemplateService.save(printTemplate);
        systemLogService.record("打印模板", SystemLog.OperationType.新增,
                "新增打印模板「" + saved.getName() + "」",
                null, saved.getDocumentType() == null ? null : saved.getDocumentType().name(),
                saved.getId(), adminId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PrintTemplate printTemplate,
                             @SaAccountBookId Long accountBookId,
                             @SaMerchantId Long merchantId,
                             @SaAdminId Long adminId) {
        PrintTemplate saved = printTemplateService.save(printTemplate);
        systemLogService.record("打印模板", SystemLog.OperationType.修改,
                "修改打印模板「" + saved.getName() + "」",
                null, saved.getDocumentType() == null ? null : saved.getDocumentType().name(),
                saved.getId(), adminId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @DeleteMapping("/{printTemplateId}")
    public JsonResult delete(@PathVariable Long printTemplateId,
                             @SaAccountBookId Long accountBookId,
                             @SaMerchantId Long merchantId,
                             @SaAdminId Long adminId) {
        printTemplateService.delete(printTemplateId, merchantId, accountBookId);
        systemLogService.record("打印模板", SystemLog.OperationType.删除,
                "删除打印模板#" + printTemplateId,
                null, null, printTemplateId, adminId, merchantId, accountBookId);
        return JsonResult.successful();
    }


    @GetMapping("/load/{printTemplateId}")
    public JsonResult load(@PathVariable Long printTemplateId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(printTemplateService.load(printTemplateId, merchantId, accountBookId));
    }

    @GetMapping("/byType")
    public JsonResult byType(String documentType, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(printTemplateService.byType(documentType, merchantId, accountBookId));
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(printTemplateService.select(merchantId, accountBookId));
    }

}
