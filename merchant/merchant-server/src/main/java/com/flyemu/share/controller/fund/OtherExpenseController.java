package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OtherExpense;
import com.flyemu.share.service.fund.OtherExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 收款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/otherExpense")
@RequiredArgsConstructor
public class OtherExpenseController {

    private final OtherExpenseService otherExpenseService;

    @GetMapping
    public JsonResult list(Page page, OtherExpenseService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherExpenseService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherExpense otherExpense, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherExpense.setMerchantId(merchantId);
        otherExpense.setAccountBookId(accountBookId);
        otherExpenseService.save(otherExpense);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherExpense otherExpense) {
        otherExpenseService.save(otherExpense);
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherExpenseId}")
    public JsonResult delete(@PathVariable Long otherExpenseId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherExpenseService.delete(otherExpenseId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(otherExpenseService.select(merchantId, accountBookId));
    }

}
