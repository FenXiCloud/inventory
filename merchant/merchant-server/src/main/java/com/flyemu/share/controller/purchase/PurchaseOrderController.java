package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseOrderForm;
import com.flyemu.share.service.purchase.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 采购订单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/purchaseOrder")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public JsonResult list(Page page, PurchaseOrderService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseOrderService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseOrderForm purchaseOrderForm, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        purchaseOrderForm.getPurchaseOrder().setMerchantId(merchantId);
        purchaseOrderForm.getPurchaseOrder().setAccountBookId(accountBookId);
        purchaseOrderForm.getPurchaseOrder().setOrderStatus(OrderStatus.已保存);
        purchaseOrderService.save(purchaseOrderForm);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseOrderForm purchaseOrderForm) {
        purchaseOrderService.save(purchaseOrderForm);
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseOrderId}")
    public JsonResult delete(@PathVariable Long purchaseOrderId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        purchaseOrderService.delete(purchaseOrderId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(purchaseOrderService.select(merchantId, accountBookId));
    }

}
