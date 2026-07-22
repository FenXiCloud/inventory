package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.AccountTransferService;
import com.flyemu.share.form.AccountTransferForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/accountTransfer")
@RequiredArgsConstructor
public class AccountTransferController {

    private final AccountTransferService accountTransferService;

    @GetMapping
    public JsonResult list(Page page, AccountTransferService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(accountTransferService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(AccountTransferService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(accountTransferService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid AccountTransferForm dto, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(dto.getOrder(), accountDto);
        accountTransferService.save(dto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid AccountTransferForm dto, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(dto.getOrder(), accountDto);
        accountTransferService.save(dto);
        return JsonResult.successful();
    }

    @DeleteMapping("/{accountTransferId}")
    public JsonResult delete(@PathVariable Long accountTransferId, @SaAccountVal AccountDto accountDto) {
        accountTransferService.delete(String.valueOf(accountTransferId), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(accountTransferService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        accountTransferService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
