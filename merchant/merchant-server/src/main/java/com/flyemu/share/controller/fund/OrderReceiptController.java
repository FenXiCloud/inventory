package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.fund.OrderReceipt;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.service.fund.OrderReceiptService;
import com.flyemu.share.form.OrderReceiptForm;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @功能描述: 收款单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/orderReceipt")
@RequiredArgsConstructor
public class OrderReceiptController {

    private final OrderReceiptService orderReceiptService;

    @GetMapping
    public JsonResult list(Page page, OrderReceiptService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.query(query, page));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OrderReceiptService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderReceiptForm orderReceipt, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        orderReceipt.getOrderReceipt().setMerchantId(merchantId);
        orderReceipt.getOrderReceipt().setAccountBookId(accountBookId);
        orderReceiptService.save(orderReceipt);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderReceiptForm orderReceipt, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        orderReceipt.getOrderReceipt().setMerchantId(merchantId);
        orderReceipt.getOrderReceipt().setAccountBookId(accountBookId);
        orderReceiptService.save(orderReceipt);
        return JsonResult.successful();
    }

    @DeleteMapping("/{orderReceiptId}")
    public JsonResult delete(@PathVariable Long orderReceiptId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        orderReceiptService.delete(String.valueOf(orderReceiptId), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(orderReceiptService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        orderReceiptService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/writeOff")
    public JsonResult writeOff(Page page, OrderReceiptService.SalesQuery query,
                               @SaMerchantId Long merchantId,
                               @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.writeOffCandidates(page, query));
    }
}
