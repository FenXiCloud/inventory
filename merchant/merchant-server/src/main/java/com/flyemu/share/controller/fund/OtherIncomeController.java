package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.OtherIncome;
import com.flyemu.share.service.fund.OtherIncomeService;
import com.flyemu.share.service.fund.dto.OrderPaymentUpdateDTO;
import com.flyemu.share.service.fund.dto.OtherIncomeDTO;
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
@RequestMapping("/otherIncome")
@RequiredArgsConstructor
public class OtherIncomeController {

    private final OtherIncomeService otherIncomeService;

    @GetMapping("list")
    public JsonResult list(Page page, OtherIncomeService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherIncomeService.query(page, query));
    }

    @PostMapping("save")
    public JsonResult save(@RequestBody @Valid OtherIncomeDTO otherIncome, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherIncome.getOrder().setMerchantId(merchantId);
        otherIncome.getOrder().setAccountBookId(accountBookId);
        otherIncomeService.save(otherIncome);
        return JsonResult.successful();
    }
    @PostMapping("updateStatus")
    public JsonResult updateStatus(@RequestBody OrderPaymentUpdateDTO orderReceipt) {
        otherIncomeService.updateStatus(orderReceipt);
        return JsonResult.successful();
    }
    @PostMapping("/delete")
    public JsonResult delete(@RequestBody OrderPaymentUpdateDTO otherIncomeId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        otherIncomeService.delete(otherIncomeId.getId(), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("selectById")
    public JsonResult selectById(Long id) {
        return JsonResult.successful(otherIncomeService.selectById(id));
    }

}
