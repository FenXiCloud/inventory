package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseReturnForm;
import com.flyemu.share.service.purchase.PurchaseInboundService;
import com.flyemu.share.service.purchase.PurchaseReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public JsonResult list(Page page, PurchaseReturnService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseReturnService.query(page, query));
    }

    /**
     * 条件内总金额
     *
     * @param merchantId
     * @param query
     * @return
     */
    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseReturnService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseReturnService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseReturnForm purchaseReturnForm, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId, @SaAdminId Long adminId) {
        purchaseReturnForm.getPurchaseReturn().setMerchantId(merchantId);
        purchaseReturnForm.getPurchaseReturn().setAccountBookId(accountBookId);
        purchaseReturnForm.getPurchaseReturn().setCreatedBy(adminId);
        purchaseReturnForm.getPurchaseReturn().setMerchantId(merchantId);
        purchaseReturnService.save(purchaseReturnForm, merchantId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseReturnForm purchaseReturnForm, @SaMerchantId Long merchantId) {
        purchaseReturnService.save(purchaseReturnForm, merchantId);
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseReturnId}")
    public JsonResult delete(@PathVariable Long purchaseReturnId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        purchaseReturnService.delete(purchaseReturnId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(purchaseReturnService.select(merchantId, accountBookId));
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
        purchaseReturnService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    /**
     * 退货单详情
     *
     * @param merchantId
     * @param orderId
     * @return
     */
    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(purchaseReturnService.load(merchantId, orderId));
    }

}
