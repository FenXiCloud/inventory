package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.OtherInbound;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.OtherInboundForm;
import com.flyemu.share.service.inventory.OtherInboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/otherInbound")
@RequiredArgsConstructor
public class OtherInboundController {

    private final OtherInboundService otherInboundService;

    @GetMapping
    public JsonResult list(Page page, OtherInboundService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(otherInboundService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherInboundForm otherInboundForm, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        OtherInbound otherInbound = otherInboundForm.getOtherInbound();
        TenantScope.bind(otherInbound, accountDto);
        otherInbound.setOrderStatus(OrderStatus.已保存);
        otherInbound.setCreatedBy(adminId);
        return JsonResult.successful(otherInboundService.save(otherInboundForm, accountDto.getMerchantId()));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherInboundForm otherInboundForm, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherInboundService.save(otherInboundForm, accountDto.getMerchantId()));
    }

    @DeleteMapping("/{otherInboundId}")
    public JsonResult delete(@PathVariable Long otherInboundId, @SaAccountVal AccountDto accountDto) {
        otherInboundService.delete(otherInboundId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherInboundService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherInboundService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        otherInboundService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
