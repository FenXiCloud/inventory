package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesReturnForm;
import com.flyemu.share.service.sales.SalesReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/salesReturn")
@RequiredArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    @GetMapping
    public JsonResult list(Page page, SalesReturnService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesReturnService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesReturnService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(salesReturnService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesReturnForm salesReturnForm,
            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(salesReturnForm.getSalesReturn(), accountDto);
        salesReturnForm.getSalesReturn().setCreatedBy(adminId);
        salesReturnForm.getSalesReturn().setCreatedAt(LocalDateTime.now());
        salesReturnForm.getSalesReturn().setOrderStatus(OrderStatus.已保存);
        salesReturnService.save(salesReturnForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesReturnForm salesReturnForm, @SaAccountVal AccountDto accountDto) {
        salesReturnService.save(salesReturnForm, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesReturnId}")
    public JsonResult delete(@PathVariable Long salesReturnId, @SaAccountVal AccountDto accountDto) {
        salesReturnService.delete(salesReturnId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReturnService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(salesReturnService.load(accountDto.getMerchantId(), orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesReturnService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
