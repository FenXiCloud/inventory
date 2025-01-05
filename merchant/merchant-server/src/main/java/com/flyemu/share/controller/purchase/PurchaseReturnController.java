package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.purchase.PurchaseReturn;
import com.flyemu.share.service.purchase.PurchaseReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 采购退货单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/purchaseReturn")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @GetMapping
    public JsonResult list(Page page, PurchaseReturnService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseReturnService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseReturn purchaseReturn, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        purchaseReturn.setMerchantId(merchantId);
        purchaseReturn.setAccountBookId(accountBookId);
        purchaseReturnService.save(purchaseReturn);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseReturn purchaseReturn) {
        purchaseReturnService.save(purchaseReturn);
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseReturnId}")
    public JsonResult delete(@PathVariable Long purchaseReturnId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        purchaseReturnService.delete(purchaseReturnId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(purchaseReturnService.select(merchantId, accountBookId));
    }

}
