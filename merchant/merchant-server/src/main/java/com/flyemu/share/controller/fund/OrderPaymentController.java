package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OrderPayment;
import com.flyemu.share.service.fund.OrderPaymentService;
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
@RequestMapping("/orderPayment")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;

    @GetMapping
    public JsonResult list(Page page, OrderPaymentService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderPayment orderPayment, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderPayment.setMerchantId(merchantId);
        orderPayment.setAccountBookId(accountBookId);
        orderPaymentService.save(orderPayment);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderPayment orderPayment) {
        orderPaymentService.save(orderPayment);
        return JsonResult.successful();
    }

    @DeleteMapping("/{orderPaymentId}")
    public JsonResult delete(@PathVariable Long orderPaymentId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderPaymentService.delete(orderPaymentId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(orderPaymentService.select(merchantId, accountBookId));
    }

}
