package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.purchase.PurchaseInbound;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseInboundForm;
import com.flyemu.share.service.purchase.PurchaseInboundService;
import com.flyemu.share.service.purchase.PurchaseOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @功能描述: 采购入库单
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
    public JsonResult list(Page page, PurchaseInboundService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseInboundService.query(page, query));
    }

    /**
     * 条件内总金额
     *
     * @param merchantId
     * @param query
     * @return
     */
    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseInboundService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseInboundService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseInboundForm purchaseInboundForm, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId, @SaAdminId Long adminId) {
        purchaseInboundForm.getPurchaseInbound().setCreatedBy(adminId);
        purchaseInboundForm.getPurchaseInbound().setMerchantId(merchantId);
        purchaseInboundForm.getPurchaseInbound().setAccountBookId(accountBookId);
        purchaseInboundForm.getPurchaseInbound().setOrderStatus(OrderStatus.已保存);
        purchaseInboundService.save(purchaseInboundForm, merchantId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseInboundForm purchaseInboundForm, @SaMerchantId Long merchantId) {
        purchaseInboundService.save(purchaseInboundForm, merchantId);
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseInboundId}")
    public JsonResult delete(@PathVariable Long purchaseInboundId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        purchaseInboundService.delete(purchaseInboundId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(purchaseInboundService.select(merchantId, accountBookId));
    }

    @GetMapping("/toReturn")
    public JsonResult listToReturn(Page page, PurchaseInboundService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseInboundService.listToReturn(page, query));
    }

    @PostMapping("/toReturn/{supplierId}")
    public JsonResult toReturn(@RequestBody List<Long> orderIds, @PathVariable Long supplierId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(purchaseInboundService.loadToReturn(orderIds, merchantId, supplierId));
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
        purchaseInboundService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    /**
     * 入库单详情
     *
     * @param merchantId
     * @param orderId
     * @return
     */
    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(purchaseInboundService.load(merchantId, orderId));
    }
}
