package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.PrintTemplate;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.service.setting.PrintTemplateService;
import com.flyemu.share.service.setting.SystemLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/printTemplate")
@RequiredArgsConstructor
public class PrintTemplateController {

    private final PrintTemplateService printTemplateService;
    private final SystemLogService systemLogService;

    @GetMapping
    public JsonResult list(PrintTemplateService.Query query, @RequestParam(required = false) String documentType,
                           @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        // 保证每个单据类型至少有一个打印模板：该类型还没有模板时自动预置一个默认模板
        printTemplateService.ensureDefaultIfMissing(documentType, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful(printTemplateService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PrintTemplate printTemplate,
                           @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(printTemplate, accountDto);
        PrintTemplate saved = printTemplateService.save(printTemplate);
        systemLogService.record("打印模板", SystemLog.OperationType.新增,
                "新增打印模板「" + saved.getName() + "」",
                null, saved.getDocumentType() == null ? null : saved.getDocumentType().name(),
                saved.getId(), adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PrintTemplate printTemplate,
                             @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        PrintTemplate saved = printTemplateService.save(printTemplate);
        systemLogService.record("打印模板", SystemLog.OperationType.修改,
                "修改打印模板「" + saved.getName() + "」",
                null, saved.getDocumentType() == null ? null : saved.getDocumentType().name(),
                saved.getId(), adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{printTemplateId}")
    public JsonResult delete(@PathVariable Long printTemplateId,
                             @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        printTemplateService.delete(printTemplateId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        systemLogService.record("打印模板", SystemLog.OperationType.删除,
                "删除打印模板#" + printTemplateId,
                null, null, printTemplateId, adminId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{printTemplateId}")
    public JsonResult load(@PathVariable Long printTemplateId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(printTemplateService.load(printTemplateId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/byType")
    public JsonResult byType(String documentType, @SaAccountVal AccountDto accountDto) {
        printTemplateService.ensureDefaultIfMissing(documentType, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful(printTemplateService.byType(documentType, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        printTemplateService.ensureDefaultsForAll(accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful(printTemplateService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
