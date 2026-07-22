package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
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

@RestController
@RequestMapping("/purchaseOrder")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @GetMapping
    public JsonResult list(Page page, PurchaseOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseOrderService.query(page, query));
    }

    @GetMapping("/toInBound")
    public JsonResult listToInBound(Page page, PurchaseOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseOrderService.queryToInBound(page, query));
    }

    /**
     * 条件内总金额
     *
     * @param query
     * @return
     */
    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseOrderService.queryTotal(query));
    }

    @PostMapping("/toInbound/{supplierId}")
    public JsonResult toInbound(@RequestBody List<Long> orderIds, @PathVariable Long supplierId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseOrderService.loadToInbound(orderIds, accountDto.getMerchantId(), supplierId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseOrderForm purchaseOrderForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        purchaseOrderForm.getPurchaseOrder().setCreatedBy(adminId);
        TenantScope.bind(purchaseOrderForm.getPurchaseOrder(), accountDto);
        purchaseOrderForm.getPurchaseOrder().setOrderStatus(OrderStatus.已保存);
        purchaseOrderService.save(purchaseOrderForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseOrderForm purchaseOrderForm, @SaAccountVal AccountDto accountDto) {
        purchaseOrderService.save(purchaseOrderForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseOrderId}")
    public JsonResult delete(@PathVariable Long purchaseOrderId, @SaAccountVal AccountDto accountDto) {
        purchaseOrderService.delete(purchaseOrderId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseOrderService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
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
     * @param orderId
     * @return
     */
    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseOrderService.load(accountDto.getMerchantId(), orderId));
    }

}
