package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SettlementForm;
import com.flyemu.share.service.fund.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/settlement")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    @GetMapping
    public JsonResult list(Page page, SettlementService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(settlementService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SettlementService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(settlementService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SettlementForm form, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form.getOrder(), accountDto);
        settlementService.save(form);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SettlementForm form, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(form.getOrder(), accountDto);
        settlementService.save(form);
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        settlementService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(settlementService.load(id, accountDto.getMerchantId()));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        settlementService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/writeOff")
    public JsonResult writeOff(Page page, SettlementService.WriteOffQuery query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(settlementService.writeOffCandidates(page, query));
    }
}
