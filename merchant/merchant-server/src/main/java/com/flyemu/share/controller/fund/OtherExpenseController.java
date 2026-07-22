package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.OtherExpenseService;
import com.flyemu.share.form.OtherExpenseForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/otherExpense")
@RequiredArgsConstructor
public class OtherExpenseController {

    private final OtherExpenseService otherExpenseService;

    @GetMapping
    public JsonResult list(Page page, OtherExpenseService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(otherExpenseService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OtherExpenseService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(otherExpenseService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherExpenseForm dto, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(dto.getOrder(), accountDto);
        otherExpenseService.save(dto.getOrder(), dto.getItemList());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherExpenseForm dto, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(dto.getOrder(), accountDto);
        otherExpenseService.save(dto.getOrder(), dto.getItemList());
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherExpenseId}")
    public JsonResult delete(@PathVariable Long otherExpenseId, @SaAccountVal AccountDto accountDto) {
        otherExpenseService.delete(String.valueOf(otherExpenseId), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(otherExpenseService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        otherExpenseService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
