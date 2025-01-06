package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.Verification;
import com.flyemu.share.service.fund.VerificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 收款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    @GetMapping
    public JsonResult list(Page page, VerificationService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(verificationService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Verification verification, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        verification.setMerchantId(merchantId);
        verification.setAccountBookId(accountBookId);
        verificationService.save(verification);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Verification verification) {
        verificationService.save(verification);
        return JsonResult.successful();
    }

    @DeleteMapping("/{verificationId}")
    public JsonResult delete(@PathVariable Long verificationId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        verificationService.delete(verificationId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(verificationService.select(merchantId, accountBookId));
    }

}
