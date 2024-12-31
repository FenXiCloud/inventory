package com.flyemu.share.controller;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.AccountType;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.service.AccountTypeService;
import com.flyemu.share.service.CustomerService;
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
    public JsonResult list(AccountTypeService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountTypeService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountType accountType, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        accountType.setMerchantId(merchantId);
        accountType.setAccountBookId(accountBookId);
        accountTypeService.save(accountType);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountType accountType) {
        accountTypeService.save(accountType);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountTypeId}")
    public JsonResult delete(@PathVariable Long accountTypeId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        accountTypeService.delete(accountTypeId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(accountTypeService.select(merchantId,accountBookId));
    }

}
