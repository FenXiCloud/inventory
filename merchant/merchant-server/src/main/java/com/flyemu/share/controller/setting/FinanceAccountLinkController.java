package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.service.setting.FinanceAccountLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

@RestController
@RequestMapping("/financeAccountLink")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceAccountLinkController {

    private final FinanceAccountLinkService financeAccountLinkService;

    @GetMapping
    public JsonResult list(FinanceAccountLinkService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(financeAccountLinkService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid FinanceAccountLink financeAccountLink, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(financeAccountLink, accountDto);
        financeAccountLinkService.save(financeAccountLink, accountDto);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid FinanceAccountLink financeAccountLink, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(financeAccountLink, accountDto);
        financeAccountLinkService.save(financeAccountLink, accountDto);
        return JsonResult.successful();
    }

    @PostMapping("/accountSets")
    public JsonResult loadAccountSetsList(@RequestBody FinanceAccountLink financeAccountLink, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadAccountSetsList(financeAccountLink, accountDto.getMerchantId()));
    }

    @GetMapping("/byAccountBook/{accountBookId}")
    public JsonResult loadByAccountBookId(@PathVariable("accountBookId") Long accountBookId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadByAccountBookId(accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable("id") Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.load(id, accountDto.getMerchantId()));
    }

    @GetMapping("/voucherWord")
    public JsonResult loadVoucherWord(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadVoucherWord(accountDto));
    }

    @GetMapping("/subject")
    public JsonResult loadSubject(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadSubject(accountDto));
    }

    @GetMapping("/accountingCategory")
    public JsonResult loadAccountingCategory(@RequestParam("ids") String ids, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadAccountingCategory(ids, accountDto));
    }

    @GetMapping("/code")
    public JsonResult loadCode(@RequestParam("word") String word,
                               @RequestParam("currentAccountDate") LocalDate currentAccountDate,
                               @SaAccountVal AccountDto accountDto) throws UnsupportedEncodingException {
        return JsonResult.successful(financeAccountLinkService.loadCode(word, currentAccountDate, accountDto));
    }

    @GetMapping("/voucherSelect")
    public JsonResult loadVoucherSelect(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadVoucherSelect(accountDto));
    }

    @GetMapping("/voucherSummary")
    public JsonResult loadVoucherSummary(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadVoucherSummary(accountDto));
    }

}
