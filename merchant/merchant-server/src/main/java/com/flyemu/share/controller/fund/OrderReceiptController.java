package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.service.fund.OrderReceiptService;
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
@RequestMapping("/orderReceipt")
@RequiredArgsConstructor
public class OrderReceiptController {

    private final OrderReceiptService orderReceiptService;

    @GetMapping
    public JsonResult list(Page page, OrderReceiptService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderReceipt orderReceipt, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderReceipt.setMerchantId(merchantId);
        orderReceipt.setAccountBookId(accountBookId);
        orderReceiptService.save(orderReceipt);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderReceipt orderReceipt) {
        orderReceiptService.save(orderReceipt);
        return JsonResult.successful();
    }

    @DeleteMapping("/{orderReceiptId}")
    public JsonResult delete(@PathVariable Long orderReceiptId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        orderReceiptService.delete(orderReceiptId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(orderReceiptService.select(merchantId, accountBookId));
    }

}
