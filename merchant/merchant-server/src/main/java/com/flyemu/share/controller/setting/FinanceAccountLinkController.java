package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.FinanceAccountLink;
import com.flyemu.share.service.setting.FinanceAccountLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.time.LocalDate;

/**
 * @功能描述: 关联云财务
 * @创建时间: 2025年03月11日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/financeAccountLink")
@RequiredArgsConstructor
@SaCheckLogin
public class FinanceAccountLinkController {

    private final FinanceAccountLinkService financeAccountLinkService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, FinanceAccountLinkService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(financeAccountLinkService.query(query));
    }


    @PostMapping("save")
    public JsonResult save(@RequestBody @Valid FinanceAccountLink financeAccountLink, @SaAccountVal AccountDto accountDto) {
        financeAccountLink.setMerchantId(accountDto.getMerchantId());
        financeAccountLink.setAccountBookId(accountDto.getAccountBookId());
        financeAccountLinkService.save(financeAccountLink, accountDto);
        return JsonResult.successful();
    }

    @PostMapping("/loadAccountSetsList")
    public JsonResult loadAccountSetsList(@RequestBody FinanceAccountLink financeAccountLink, @SaMerchantId Long merchantId) {
        return JsonResult.successful(financeAccountLinkService.loadAccountSetsList(financeAccountLink, merchantId));
    }

    @GetMapping("/loadByAccountBookId/{accountBookId}")
    public JsonResult loadByAccountBookId(@PathVariable("accountBookId") Long accountBookId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(financeAccountLinkService.loadByAccountBookId(accountBookId));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable("id") Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(financeAccountLinkService.load(id, merchantId));
    }


    @GetMapping("/loadVoucherWord")
    public JsonResult loadVoucherWord(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadVoucherWord(accountDto));
    }

    @GetMapping("/loadSubject")
    public JsonResult loadSubject(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadSubject(accountDto));
    }

    @GetMapping("/loadAccountingCategory")
    public JsonResult loadAccountingCategory(@RequestParam("ids") String ids, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadAccountingCategory(ids, accountDto));
    }

    @GetMapping("/loadCode")
    public JsonResult loadCode(@RequestParam("word") String word,
                               @RequestParam("currentAccountDate") LocalDate currentAccountDate,
                               @SaAccountVal AccountDto accountDto) throws UnsupportedEncodingException {
        return JsonResult.successful(financeAccountLinkService.loadCode(word, currentAccountDate, accountDto));
    }

    @GetMapping("/loadVoucherSelect")
    public JsonResult loadVoucherSelect(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadVoucherSelect(accountDto));
    }

    @GetMapping("/loadVoucherSummary")
    public JsonResult loadVoucherSummary(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(financeAccountLinkService.loadVoucherSummary(accountDto));
    }

}
