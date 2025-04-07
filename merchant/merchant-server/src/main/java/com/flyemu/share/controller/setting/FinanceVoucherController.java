package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceVoucher;
import com.flyemu.share.entity.setting.FinanceVoucherTemplate;
import com.flyemu.share.service.setting.FinanceVoucherService;
import com.flyemu.share.service.setting.FinanceVoucherTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 云财务凭证
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/financeVoucher")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceVoucherController {

    private final FinanceVoucherService financeVoucherService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, FinanceVoucherService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(financeVoucherService.query(query));
    }

    @PostMapping("save")
    public JsonResult save(@RequestBody @Valid FinanceVoucher financeVoucher, @SaAccountVal AccountDto accountDto) {
        financeVoucher.setMerchantId(accountDto.getMerchantId());
        financeVoucher.setAccountBookId(accountDto.getAccountBookId());
        financeVoucherService.save(financeVoucher, accountDto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        financeVoucherService.delete(id, merchantId, accountBookId);
        return JsonResult.successful();
    }


    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id) {
        return JsonResult.successful(financeVoucherService.load(id));
    }

}
