package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.AccountType;
import com.flyemu.share.service.basic.AccountTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 账户收支类别
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/accountType")
@RequiredArgsConstructor
public class AccountTypeController {

    private final AccountTypeService accountTypeService;

    @GetMapping
    public JsonResult list(AccountTypeService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountTypeService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountType accountType, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        accountType.setMerchantId(merchantId);
        accountType.setAccountBookId(accountBookId);
        accountTypeService.save(accountType);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountType accountType, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        accountType.setMerchantId(merchantId);
        accountType.setAccountBookId(accountBookId);
        accountTypeService.save(accountType);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountTypeId}")
    public JsonResult delete(@PathVariable Long accountTypeId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        accountTypeService.delete(accountTypeId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(accountTypeService.select(merchantId, accountBookId));
    }

}
