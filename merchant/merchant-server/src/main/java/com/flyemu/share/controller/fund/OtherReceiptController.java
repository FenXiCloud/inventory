package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.OtherReceiptService;
import com.flyemu.share.form.OtherReceiptForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/otherReceipt")
@RequiredArgsConstructor
public class OtherReceiptController {

    private final OtherReceiptService otherReceiptService;

    @GetMapping
    public JsonResult list(Page page, OtherReceiptService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(otherReceiptService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OtherReceiptService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(otherReceiptService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherReceiptForm otherReceipt, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(otherReceipt.getOrder(), accountDto);
        otherReceiptService.save(otherReceipt);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherReceiptForm otherReceipt, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(otherReceipt.getOrder(), accountDto);
        otherReceiptService.save(otherReceipt);
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherReceiptId}")
    public JsonResult delete(@PathVariable Long otherReceiptId, @SaAccountVal AccountDto accountDto) {
        otherReceiptService.delete(String.valueOf(otherReceiptId), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherReceiptService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        otherReceiptService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
