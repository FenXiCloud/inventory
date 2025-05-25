package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
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
import java.util.Map;

/**
 * @功能描述: 云财务凭证
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/financeVoucher")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceVoucherController {

    private final FinanceVoucherService financeVoucherService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, FinanceVoucherService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(financeVoucherService.query(query));
    }

    @PostMapping("save")
    public JsonResult batchSave(@RequestBody @Valid FinanceVoucherForm financeVoucherForm, @SaAccountVal AccountDto accountDto) throws JsonProcessingException, UnsupportedEncodingException {
        financeVoucherForm.setMerchantId(accountDto.getMerchantId());
        financeVoucherForm.setAccountBookId(accountDto.getAccountBookId());
        financeVoucherService.save(financeVoucherForm);
        return JsonResult.successful();
    }

    @PostMapping("upVoucher")
    public JsonResult upVoucher(@RequestBody @Valid VoucherDto voucherDto, @SaAccountVal AccountDto accountDto) throws JsonProcessingException, UnsupportedEncodingException {
        financeVoucherService.upVoucher(voucherDto, accountDto);
        return JsonResult.successful();
    }

    @GetMapping("balance")
    public JsonResult balance(String subjectId, String categoryId, String categoryDetailsId, @SaAccountVal AccountDto accountDto) {
        Double balance = financeVoucherService.balance(subjectId, categoryId, categoryDetailsId, accountDto);
        return JsonResult.successful(balance);
    }

    @GetMapping("loadAuxiliaryAccountingData")
    public JsonResult loadAuxiliaryAccountingData(String ids, @SaAccountVal AccountDto accountDto) {
        List<String> categories = Arrays.stream(ids.split(",")).toList();
        Object auxiliaryAccountingData = financeVoucherService.loadAuxiliaryAccountingData(categories, accountDto);
        return JsonResult.successful(auxiliaryAccountingData);
    }


    @GetMapping("loadVoucher")
    public JsonResult loadVoucher(String voucherId, @SaAccountVal AccountDto accountDto) {
        Object voucher = financeVoucherService.loadVoucher(voucherId, accountDto);
        return JsonResult.successful(voucher);
    }

    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        financeVoucherService.delete(id, merchantId, accountBookId);
        return JsonResult.successful();
    }


    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id) {
        return JsonResult.successful(financeVoucherService.load(id));
    }

}
