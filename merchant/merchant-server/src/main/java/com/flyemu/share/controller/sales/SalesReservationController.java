package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesReservationForm;
import com.flyemu.share.service.sales.SalesReservationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/salesReservation")
@RequiredArgsConstructor
public class SalesReservationController {

    private final SalesReservationService salesReservationService;

    @GetMapping
    public JsonResult list(Page page, SalesReservationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesReservationService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesReservationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesReservationService.queryTotal(query));
    }

    @GetMapping("/toPurchase")
    public JsonResult listToPurchase(Page page, SalesReservationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesReservationService.queryToPurchase(page, query));
    }

    @PostMapping("/toPurchase")
    public JsonResult toPurchase(@RequestBody List<Long> reservationIds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReservationService.loadToPurchase(reservationIds, accountDto.getMerchantId()));
    }

    @PostMapping("/toSalesOrder")
    public JsonResult toSalesOrder(@RequestBody List<Long> reservationIds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReservationService.loadToSalesOrder(reservationIds, accountDto.getMerchantId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesReservationForm salesReservationForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(salesReservationForm.getSalesReservation(), accountDto);
        salesReservationForm.getSalesReservation().setCreatedBy(adminId);
        salesReservationForm.getSalesReservation().setCreatedAt(LocalDateTime.now());
        salesReservationForm.getSalesReservation().setOrderStatus(OrderStatus.已保存);
        salesReservationService.save(salesReservationForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesReservationForm salesReservationForm, @SaAccountVal AccountDto accountDto) {
        salesReservationService.save(salesReservationForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesReservationId}")
    public JsonResult delete(@PathVariable Long salesReservationId, @SaAccountVal AccountDto accountDto) {
        salesReservationService.delete(salesReservationId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReservationService.load(accountDto.getMerchantId(), orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesReservationService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PostMapping("/transferToPurchase/{id}")
    public JsonResult transferToPurchase(@PathVariable Long id, @RequestParam Long supplierId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReservationService.transferToPurchase(id, supplierId, accountDto.getMerchantId(), accountDto.getAdminId()));
    }

    @PostMapping("/transferToSalesOrder/{id}")
    public JsonResult transferToSalesOrder(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReservationService.transferToSalesOrder(id, accountDto.getMerchantId(), accountDto.getAdminId()));
    }
}
