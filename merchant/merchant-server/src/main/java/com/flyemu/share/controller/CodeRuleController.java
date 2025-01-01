package com.flyemu.share.controller;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.entity.setting.CodeRule;
import com.flyemu.share.service.CodeRuleService;
import com.flyemu.share.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 编码规则
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/codeRule")
@RequiredArgsConstructor
public class CodeRuleController {

    private final CodeRuleService codeRuleService;

    @GetMapping
    public JsonResult list(CodeRuleService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(codeRuleService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CodeRule codeRule, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        codeRule.setMerchantId(merchantId);
        codeRule.setAccountBookId(accountBookId);
        codeRuleService.save(codeRule);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CodeRule codeRule) {
        codeRuleService.save(codeRule);
        return JsonResult.successful();
    }

    @DeleteMapping("/{codeRuleId}")
    public JsonResult delete(@PathVariable Long codeRuleId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        codeRuleService.delete(codeRuleId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(codeRuleService.select(merchantId,accountBookId));
    }

}
