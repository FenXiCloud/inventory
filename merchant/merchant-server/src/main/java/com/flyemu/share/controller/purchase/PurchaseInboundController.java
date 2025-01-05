package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.service.purchase.PurchaseInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 销售出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/purchaseInbound")
@RequiredArgsConstructor
public class PurchaseInboundController {

    private final PurchaseInboundService purchaseInboundService;

    @GetMapping
    public JsonResult list(Page page, PurchaseInboundService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseInboundService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseInbound purchaseInbound, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        purchaseInbound.setMerchantId(merchantId);
        purchaseInbound.setAccountBookId(accountBookId);
        purchaseInboundService.save(purchaseInbound);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseInbound purchaseInbound) {
        purchaseInboundService.save(purchaseInbound);
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseInboundId}")
    public JsonResult delete(@PathVariable Long purchaseInboundId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        purchaseInboundService.delete(purchaseInboundId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(purchaseInboundService.select(merchantId, accountBookId));
    }

}
