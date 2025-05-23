package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.Verification;
import com.flyemu.share.service.fund.VerificationService;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.dto.VerificationSaveDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public JsonResult list(Page page, VerificationService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(verificationService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody VerificationSaveDTO verification, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        verification.getOrder().setMerchantId(merchantId);
        verification.getOrder().setAccountBookId(accountBookId);
        verificationService.save(verification);
        return JsonResult.successful();
    }


    @PostMapping("updateStatus")
    public JsonResult updateStatus(@RequestBody OrderPaymentUpdateDTO orderReceipt) {
        verificationService.updateStatus(orderReceipt);
        return JsonResult.successful();
    }
    @PostMapping("delete")
    public JsonResult delete(@RequestBody OrderPaymentUpdateDTO verification, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        verificationService.delete(verification.getId(), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("selectById")
    public JsonResult selectById(Long id) {
        return JsonResult.successful(verificationService.selectById(id));
    }


}
