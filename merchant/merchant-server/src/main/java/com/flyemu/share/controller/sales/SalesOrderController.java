package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.service.sales.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/salesOrder")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    public JsonResult list(Page page, SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.queryTotal(query));
    }

    @GetMapping("/toOutBound")
    public JsonResult listToOutBound(Page page, SalesOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOrderService.queryToOutBound(page, query));
    }

    @PostMapping("/toOutbound/{customerId}")
    public JsonResult toOutbound(@RequestBody List<Long> orderIds, @PathVariable Long customerId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.loadToOutbound(orderIds, accountDto.getMerchantId(), customerId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesOrderForm salesOrderForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(salesOrderForm.getSalesOrder(), accountDto);
        salesOrderForm.getSalesOrder().setCreatedBy(adminId);
        salesOrderForm.getSalesOrder().setCreatedAt(LocalDateTime.now());
        salesOrderForm.getSalesOrder().setOrderStatus(OrderStatus.已保存);
        salesOrderService.save(salesOrderForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOrderForm salesOrderForm, @SaAccountVal AccountDto accountDto) {
        salesOrderService.save(salesOrderForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOrderId}")
    public JsonResult delete(@PathVariable Long salesOrderId, @SaAccountVal AccountDto accountDto) {
        salesOrderService.delete(salesOrderId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOrderService.load(accountDto.getMerchantId(), orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesOrderService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
