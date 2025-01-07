package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.PaymentMethod;
import com.flyemu.share.service.basic.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 结算方式
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/paymentMethod")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public JsonResult list(PaymentMethodService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(paymentMethodService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PaymentMethod paymentMethod, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        paymentMethod.setMerchantId(merchantId);
        paymentMethod.setAccountBookId(accountBookId);
        paymentMethod.setEnabled(true);
        paymentMethodService.save(paymentMethod);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PaymentMethod paymentMethod) {
        paymentMethodService.save(paymentMethod);
        return JsonResult.successful();
    }

    @DeleteMapping("/{paymentMethodId}")
    public JsonResult delete(@PathVariable Long paymentMethodId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        paymentMethodService.delete(paymentMethodId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(paymentMethodService.select(merchantId,accountBookId));
    }

}
