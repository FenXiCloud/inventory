package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.VerificationService;
import com.flyemu.share.form.VerificationForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
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
    public JsonResult list(Page page, VerificationService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(verificationService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(VerificationService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(verificationService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid VerificationForm verification, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        verification.getOrder().setMerchantId(merchantId);
        verification.getOrder().setAccountBookId(accountBookId);
        verificationService.save(verification);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid VerificationForm verification, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        verification.getOrder().setMerchantId(merchantId);
        verification.getOrder().setAccountBookId(accountBookId);
        verificationService.save(verification);
        return JsonResult.successful();
    }

    @DeleteMapping("/{verificationId}")
    public JsonResult delete(@PathVariable Long verificationId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        verificationService.delete(String.valueOf(verificationId), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(verificationService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        verificationService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
