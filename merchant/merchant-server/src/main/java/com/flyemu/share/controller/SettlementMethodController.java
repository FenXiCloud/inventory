package com.flyemu.share.controller;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Customer;
import com.flyemu.share.entity.basic.SettlementMethod;
import com.flyemu.share.service.CustomerService;
import com.flyemu.share.service.SettlementMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 结算方式
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/settlementMethod")
@RequiredArgsConstructor
public class SettlementMethodController {

    private final SettlementMethodService settlementMethodService;

    @GetMapping
    public JsonResult list(SettlementMethodService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(settlementMethodService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SettlementMethod settlementMethod, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        settlementMethod.setMerchantId(merchantId);
        settlementMethod.setAccountBookId(accountBookId);
        settlementMethodService.save(settlementMethod);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SettlementMethod settlementMethod) {
        settlementMethodService.save(settlementMethod);
        return JsonResult.successful();
    }

    @DeleteMapping("/{settlementMethodId}")
    public JsonResult delete(@PathVariable Long settlementMethodId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        settlementMethodService.delete(settlementMethodId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(settlementMethodService.select(merchantId,accountBookId));
    }

}
