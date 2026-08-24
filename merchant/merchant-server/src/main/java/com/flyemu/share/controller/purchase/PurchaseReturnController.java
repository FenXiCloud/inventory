package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseReturnForm;
import com.flyemu.share.service.purchase.PurchaseReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/purchaseReturn")
@RequiredArgsConstructor
public class PurchaseReturnController {

    private final PurchaseReturnService purchaseReturnService;

    @GetMapping
    public JsonResult list(Page page, PurchaseReturnService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReturnService.query(page, query));
    }

    /**
     * 条件内总金额
     *
     * @param query
     * @return
     */
    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseReturnService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReturnService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseReturnForm purchaseReturnForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(purchaseReturnForm.getPurchaseReturn(), accountDto);
        purchaseReturnForm.getPurchaseReturn().setCreatedBy(adminId);
        purchaseReturnService.save(purchaseReturnForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseReturnForm purchaseReturnForm, @SaAccountVal AccountDto accountDto) {
        purchaseReturnService.save(purchaseReturnForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseReturnId}")
    public JsonResult delete(@PathVariable Long purchaseReturnId, @SaAccountVal AccountDto accountDto) {
        purchaseReturnService.delete(purchaseReturnId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseReturnService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    /**
     * 批量审核
     *
     * @param ids
     * @param state
     * @param accountDto
     * @return
     */
    //审核
    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        purchaseReturnService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    /**
     * 退货单详情
     *
     * @param orderId
     * @return
     */
    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseReturnService.load(accountDto.getMerchantId(), orderId));
    }

}
