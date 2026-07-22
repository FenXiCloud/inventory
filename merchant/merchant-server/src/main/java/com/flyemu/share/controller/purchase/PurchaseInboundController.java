package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseInboundForm;
import com.flyemu.share.service.purchase.PurchaseInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchaseInbound")
@RequiredArgsConstructor
public class PurchaseInboundController {

    private final PurchaseInboundService purchaseInboundService;

    @GetMapping
    public JsonResult list(Page page, PurchaseInboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseInboundService.query(page, query));
    }

    /**
     * 条件内总金额
     *
     * @param query
     * @return
     */
    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseInboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseInboundService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseInboundForm purchaseInboundForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        purchaseInboundForm.getPurchaseInbound().setCreatedBy(adminId);
        TenantScope.bind(purchaseInboundForm.getPurchaseInbound(), accountDto);
        purchaseInboundForm.getPurchaseInbound().setOrderStatus(OrderStatus.已保存);
        purchaseInboundService.save(purchaseInboundForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseInboundForm purchaseInboundForm, @SaAccountVal AccountDto accountDto) {
        purchaseInboundService.save(purchaseInboundForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseInboundId}")
    public JsonResult delete(@PathVariable Long purchaseInboundId, @SaAccountVal AccountDto accountDto) {
        purchaseInboundService.delete(purchaseInboundId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseInboundService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/toReturn")
    public JsonResult listToReturn(Page page, PurchaseInboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseInboundService.listToReturn(page, query));
    }

    @PostMapping("/toReturn/{supplierId}")
    public JsonResult toReturn(@RequestBody List<Long> orderIds, @PathVariable Long supplierId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseInboundService.loadToReturn(orderIds, accountDto.getMerchantId(), supplierId));
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
     * @param orderId
     * @return
     */
    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseInboundService.load(accountDto.getMerchantId(), orderId));
    }
}
