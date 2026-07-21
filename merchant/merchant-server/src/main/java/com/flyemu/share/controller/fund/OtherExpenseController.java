package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
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

/**
 * @功能描述: 其他支出单
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
    public JsonResult list(Page page, OtherExpenseService.Query query,
                           @SaMerchantId Long merchantId,
                           @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherExpenseService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OtherExpenseService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherExpenseService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherExpenseForm dto,
                           @SaMerchantId Long merchantId,
                           @SaAccountBookId Long accountBookId) {
        dto.getOrder().setMerchantId(merchantId);
        dto.getOrder().setAccountBookId(accountBookId);
        otherExpenseService.save(dto.getOrder(), dto.getItemList());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherExpenseForm dto,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId) {
        dto.getOrder().setMerchantId(merchantId);
        dto.getOrder().setAccountBookId(accountBookId);
        otherExpenseService.save(dto.getOrder(), dto.getItemList());
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherExpenseId}")
    public JsonResult delete(@PathVariable Long otherExpenseId,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId) {
        otherExpenseService.delete(String.valueOf(otherExpenseId), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(otherExpenseService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        otherExpenseService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
