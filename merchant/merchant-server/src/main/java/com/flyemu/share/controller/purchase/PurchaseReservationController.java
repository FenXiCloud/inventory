package com.flyemu.share.controller.purchase;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.PurchaseReservationForm;
import com.flyemu.share.service.purchase.PurchaseReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/purchaseReservation")
@RequiredArgsConstructor
public class PurchaseReservationController {

    private final PurchaseReservationService purchaseReservationService;

    @GetMapping
    public JsonResult list(Page page, PurchaseReservationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReservationService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(PurchaseReservationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReservationService.queryTotal(query));
    }

    @GetMapping("/toPurchaseOrder")
    public JsonResult listToPurchaseOrder(Page page, PurchaseReservationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReservationService.queryToPurchaseOrder(page, query));
    }

    @PostMapping("/toPurchaseOrder")
    public JsonResult toPurchaseOrder(@RequestBody List<Long> reservationIds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseReservationService.loadToPurchaseOrder(reservationIds, accountDto.getMerchantId()));
    }

    @PostMapping("/toPurchaseInbound")
    public JsonResult toPurchaseInbound(@RequestBody List<Long> reservationIds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseReservationService.loadToPurchaseInbound(reservationIds, accountDto.getMerchantId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid PurchaseReservationForm purchaseReservationForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(purchaseReservationForm.getPurchaseReservation(), accountDto);
        purchaseReservationForm.getPurchaseReservation().setCreatedBy(adminId);
        purchaseReservationForm.getPurchaseReservation().setCreatedAt(LocalDateTime.now());
        purchaseReservationForm.getPurchaseReservation().setOrderStatus(OrderStatus.已保存);
        purchaseReservationService.save(purchaseReservationForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid PurchaseReservationForm purchaseReservationForm, @SaAccountVal AccountDto accountDto) {
        purchaseReservationService.save(purchaseReservationForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{purchaseReservationId}")
    public JsonResult delete(@PathVariable Long purchaseReservationId, @SaAccountVal AccountDto accountDto) {
        purchaseReservationService.delete(purchaseReservationId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseReservationService.load(accountDto.getMerchantId(), orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        purchaseReservationService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PostMapping("/transferToPurchaseInbound/{id}")
    public JsonResult transferToPurchaseInbound(@PathVariable Long id, @RequestParam(required = false) Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(purchaseReservationService.transferToPurchaseInbound(id, warehouseId, accountDto.getMerchantId(), accountDto.getAdminId()));
    }
}
