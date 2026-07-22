package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.OtherOutbound;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.OtherOutboundForm;
import com.flyemu.share.service.inventory.OtherOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/otherOutbound")
@RequiredArgsConstructor
public class OtherOutboundController {

    private final OtherOutboundService otherOutboundService;

    @GetMapping
    public JsonResult list(Page page, OtherOutboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(otherOutboundService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherOutboundForm otherOutboundForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        OtherOutbound otherOutbound = otherOutboundForm.getOtherOutbound();
        TenantScope.bind(otherOutbound, accountDto);
        otherOutbound.setOrderStatus(OrderStatus.已保存);
        otherOutbound.setCreatedBy(adminId);
        return JsonResult.successful(otherOutboundService.save(otherOutboundForm, accountDto.getMerchantId()));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherOutboundForm otherOutboundForm, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherOutboundService.save(otherOutboundForm, accountDto.getMerchantId()));
    }

    @DeleteMapping("/{otherOutboundId}")
    public JsonResult delete(@PathVariable Long otherOutboundId, @SaAccountVal AccountDto accountDto) {
        otherOutboundService.delete(otherOutboundId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherOutboundService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherOutboundService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        otherOutboundService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
