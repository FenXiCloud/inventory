package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.VoucherDto;
import com.flyemu.share.form.FinanceVoucherForm;
import com.flyemu.share.service.setting.FinanceVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/financeVoucher")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceVoucherController {

    private final FinanceVoucherService financeVoucherService;

    @GetMapping
    public JsonResult list(FinanceVoucherService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(financeVoucherService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid FinanceVoucherForm financeVoucherForm, @SaAccountVal AccountDto accountDto) throws JsonProcessingException, UnsupportedEncodingException {
        TenantScope.bind(financeVoucherForm, accountDto);
        financeVoucherService.save(financeVoucherForm);
        return JsonResult.successful();
    }

    @PostMapping("/sync")
    public JsonResult sync(@RequestBody @Valid VoucherDto voucherDto, @SaAccountVal AccountDto accountDto) throws JsonProcessingException, UnsupportedEncodingException {
        financeVoucherService.upVoucher(voucherDto, accountDto);
        return JsonResult.successful();
    }

    @GetMapping("/balance")
    public JsonResult balance(String subjectId, String categoryId, String categoryDetailsId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeVoucherService.balance(subjectId, categoryId, categoryDetailsId, accountDto));
    }

    @GetMapping("/auxiliary")
    public JsonResult auxiliary(String ids, @SaAccountVal AccountDto accountDto) {
        List<String> categories = Arrays.stream(ids.split(",")).toList();
        return JsonResult.successful(financeVoucherService.loadAuxiliaryAccountingData(categories, accountDto));
    }

    @GetMapping("/remote")
    public JsonResult remote(String voucherId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeVoucherService.loadVoucher(voucherId, accountDto));
    }

    @GetMapping("/candidates")
    public JsonResult candidates(Page page, FinanceVoucherService.CandidateQuery query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(financeVoucherService.candidates(page, query));
    }

    @PostMapping("/batch")
    public JsonResult batchGenerate(@RequestBody List<FinanceVoucherForm> forms, @SaAccountVal AccountDto accountDto) throws JsonProcessingException, UnsupportedEncodingException {
        financeVoucherService.batchGenerate(forms, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/batch")
    public JsonResult batchDelete(@RequestBody List<Long> ids, @SaAccountVal AccountDto accountDto) {
        financeVoucherService.batchDelete(ids, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        financeVoucherService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeVoucherService.load(accountDto.getMerchantId(), id));
    }

}
