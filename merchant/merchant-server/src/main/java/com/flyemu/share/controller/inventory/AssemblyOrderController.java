package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.AssemblyOrderForm;
import com.flyemu.share.service.inventory.AssemblyOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 组装拆卸单：审核联动库存与成本。
 */
@RestController
@RequestMapping("/assemblyOrder")
@RequiredArgsConstructor
public class AssemblyOrderController {

    private final AssemblyOrderService assemblyOrderService;

    @GetMapping
    public JsonResult list(Page page, AssemblyOrderService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(assemblyOrderService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody AssemblyOrderForm form, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        form.getAssemblyOrder().setCreatedBy(adminId);
        TenantScope.bind(form.getAssemblyOrder(), accountDto);
        assemblyOrderService.save(form, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody AssemblyOrderForm form, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form.getAssemblyOrder(), accountDto);
        assemblyOrderService.save(form, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{assemblyOrderId}")
    public JsonResult delete(@PathVariable Long assemblyOrderId, @SaAccountVal AccountDto accountDto) {
        assemblyOrderService.delete(assemblyOrderId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        assemblyOrderService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(assemblyOrderService.load(accountDto.getMerchantId(), orderId));
    }
}
