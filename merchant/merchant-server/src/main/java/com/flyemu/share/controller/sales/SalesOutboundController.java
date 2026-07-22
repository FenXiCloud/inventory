package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.service.sales.SalesOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/salesOutbound")
@RequiredArgsConstructor
public class SalesOutboundController {

    private final SalesOutboundService salesOutboundService;

    @GetMapping
    public JsonResult list(Page page, SalesOutboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOutboundService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesOutboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesOutboundService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesOutboundForm salesOutboundForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(salesOutboundForm.getSalesOutbound(), accountDto);
        salesOutboundForm.getSalesOutbound().setCreatedBy(adminId);
        salesOutboundForm.getSalesOutbound().setCreatedAt(LocalDateTime.now());
        salesOutboundForm.getSalesOutbound().setOrderStatus(OrderStatus.已保存);
        salesOutboundService.save(salesOutboundForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOutboundForm salesOutboundForm, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.save(salesOutboundForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOutboundId}")
    public JsonResult delete(@PathVariable Long salesOutboundId, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.delete(salesOutboundId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOutboundService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesOutboundService.load(accountDto.getMerchantId(), orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
