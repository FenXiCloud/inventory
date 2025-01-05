package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.Account;
import com.flyemu.share.service.basic.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 账户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping
    public JsonResult list(AccountService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Account account, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        account.setMerchantId(merchantId);
        account.setAccountBookId(accountBookId);
        accountService.save(account);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Account account) {
        accountService.save(account);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountId}")
    public JsonResult delete(@PathVariable Long accountId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        accountService.delete(accountId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(accountService.select(merchantId,accountBookId));
    }

}
