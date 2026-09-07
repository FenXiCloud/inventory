package com.flyemu.share.controller.setting;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.service.setting.CodeRuleService;
import com.flyemu.share.service.setting.CodeSeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/codeRule")
@RequiredArgsConstructor
public class CodeRuleController {

    private final CodeRuleService codeRuleService;
    private final CodeSeedService codeSeedService;

    @GetMapping
    public JsonResult list(CodeRuleService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(codeRuleService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CodeRule codeRule, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(codeRule, accountDto);
        codeRuleService.save(codeRule);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CodeRule codeRule, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(codeRule, accountDto);
        codeRuleService.save(codeRule);
        return JsonResult.successful();
    }

    @DeleteMapping("/{codeRuleId}")
    public JsonResult delete(@PathVariable Long codeRuleId, @SaAccountVal AccountDto accountDto) {
        codeRuleService.delete(codeRuleId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(codeRuleService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    /**
     * 编码规则效果预览（只读，不消耗流水号）：草稿规则字段 + 当前账套流水进度
     */
    @PostMapping("/preview")
    public JsonResult preview(@RequestBody CodeRule draft, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(draft, accountDto);
        return JsonResult.successful(codeSeedService.preview(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), draft));
    }

}
