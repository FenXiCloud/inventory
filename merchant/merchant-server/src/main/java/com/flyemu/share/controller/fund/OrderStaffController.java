package com.flyemu.share.controller.fund;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OrderStaff;
import com.flyemu.share.service.fund.OrderStaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 业务员
 */
@RestController
@RequestMapping("/orderStaff")
@RequiredArgsConstructor
public class OrderStaffController {

    private final OrderStaffService orderStaffService;

    @GetMapping
    public JsonResult list(Page page, OrderStaffService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(orderStaffService.query(page, query));
    }

    @GetMapping("/select")
    public JsonResult select(OrderStaffService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(orderStaffService.select(query));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Integer id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(orderStaffService.load(id, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderStaff orderStaff, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(orderStaff, accountDto);
        orderStaffService.save(orderStaff);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderStaff orderStaff, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(orderStaff, accountDto);
        orderStaffService.save(orderStaff);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Integer id, @SaAccountVal AccountDto accountDto) {
        orderStaffService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
