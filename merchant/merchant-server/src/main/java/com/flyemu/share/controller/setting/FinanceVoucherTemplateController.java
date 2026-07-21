package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceVoucherTemplate;
import com.flyemu.share.service.setting.FinanceVoucherTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 云财务凭证模板
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/financeVoucherTemplate")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceVoucherTemplateController {

    private final FinanceVoucherTemplateService financeVoucherTemplateService;

    @GetMapping
    public JsonResult list(FinanceVoucherTemplateService.Query query, @SaAccountVal AccountDto accountDto) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(financeVoucherTemplateService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid FinanceVoucherTemplate financeVoucherTemplate, @SaAccountVal AccountDto accountDto) {
        financeVoucherTemplate.setMerchantId(accountDto.getMerchantId());
        financeVoucherTemplate.setAccountBookId(accountDto.getAccountBookId());
        financeVoucherTemplateService.save(financeVoucherTemplate, accountDto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid FinanceVoucherTemplate financeVoucherTemplate, @SaAccountVal AccountDto accountDto) {
        financeVoucherTemplate.setMerchantId(accountDto.getMerchantId());
        financeVoucherTemplate.setAccountBookId(accountDto.getAccountBookId());
        financeVoucherTemplateService.save(financeVoucherTemplate, accountDto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        financeVoucherTemplateService.delete(id, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(financeVoucherTemplateService.load(merchantId, id));
    }

}
