package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.OtherReceiptService;
import com.flyemu.share.form.OtherReceiptForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @功能描述: 其他收入单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/otherReceipt")
@RequiredArgsConstructor
public class OtherReceiptController {

    private final OtherReceiptService otherReceiptService;

    @GetMapping
    public JsonResult list(Page page, OtherReceiptService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherReceiptService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OtherReceiptService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(otherReceiptService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OtherReceiptForm otherReceipt, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        otherReceipt.getOrder().setMerchantId(merchantId);
        otherReceipt.getOrder().setAccountBookId(accountBookId);
        otherReceiptService.save(otherReceipt);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OtherReceiptForm otherReceipt, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        otherReceipt.getOrder().setMerchantId(merchantId);
        otherReceipt.getOrder().setAccountBookId(accountBookId);
        otherReceiptService.save(otherReceipt);
        return JsonResult.successful();
    }

    @DeleteMapping("/{otherReceiptId}")
    public JsonResult delete(@PathVariable Long otherReceiptId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        otherReceiptService.delete(String.valueOf(otherReceiptId), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(otherReceiptService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        otherReceiptService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
