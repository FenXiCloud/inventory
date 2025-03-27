package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseOrderForm;
import com.flyemu.share.service.purchase.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    /**
     * 条件内总金额
     *
     * @param merchantId
     * @param query
     * @return
     */
    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseOrderService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseOrderService.queryTotal(query));
    }

    @GetMapping("/toReturn")
    public JsonResult listToReturn(Page page, PurchaseOrderService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseOrderService.listToReturn(page, query));
    }

    @PostMapping("/toInbound/{supplierId}")
    public JsonResult toInbound(@RequestBody List<Long> orderIds, @PathVariable Long supplierId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(purchaseOrderService.loadToInbound(orderIds, merchantId, supplierId));
    }

    @PostMapping("/toReturn/{supplierId}")
    public JsonResult toReturn(@RequestBody List<Long> orderIds, @PathVariable Long supplierId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(purchaseOrderService.loadToReturn(orderIds, merchantId, supplierId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseOrderForm purchaseOrderForm, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId, @SaAdminId Long adminId) {
        purchaseOrderForm.getPurchaseOrder().setCreatedBy(adminId);
        purchaseOrderForm.getPurchaseOrder().setMerchantId(merchantId);
        purchaseOrderForm.getPurchaseOrder().setAccountBookId(accountBookId);
        purchaseOrderForm.getPurchaseOrder().setOrderStatus(OrderStatus.已保存);
        purchaseOrderService.save(purchaseOrderForm, merchantId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseOrderForm purchaseOrderForm, @SaMerchantId Long merchantId) {
        purchaseOrderService.save(purchaseOrderForm, merchantId);
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

    /**
     * 批量审核
     *
     * @param ids
     * @param state
     * @param accountDto
     * @return
     */
    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        purchaseOrderService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }


    /**
     * 采购单详情
     *
     * @param merchantId
     * @param orderId
     * @return
     */
    @GetMapping("load/{orderId}")
    public JsonResult load(@SaMerchantId Long merchantId, @PathVariable Long orderId) {
        return JsonResult.successful(purchaseOrderService.load(merchantId, orderId));
    }

}
