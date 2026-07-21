package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.fund.OrderPayment;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.OrderPaymentForm;
import com.flyemu.share.service.fund.OrderPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/orderPayment")
@RequiredArgsConstructor
public class OrderPaymentController {

    private final OrderPaymentService orderPaymentService;

    @GetMapping
    public JsonResult list(Page page, OrderPaymentService.Query query,
                           @SaMerchantId Long merchantId,
                           @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.query(query, page));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(OrderPaymentService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid OrderPaymentForm orderPaymentForm,
                           @SaMerchantId Long merchantId,
                           @SaAccountBookId Long accountBookId) {
        OrderPayment orderPayment = orderPaymentForm.getOrderPayment();
        orderPayment.setMerchantId(merchantId);
        orderPayment.setAccountBookId(accountBookId);
        orderPaymentService.save(orderPaymentForm);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid OrderPaymentForm orderPaymentForm,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId) {
        OrderPayment orderPayment = orderPaymentForm.getOrderPayment();
        orderPayment.setMerchantId(merchantId);
        orderPayment.setAccountBookId(accountBookId);
        orderPaymentService.save(orderPaymentForm);
        return JsonResult.successful();
    }

    @DeleteMapping("/{orderPaymentId}")
    public JsonResult delete(@PathVariable Long orderPaymentId,
                             @SaMerchantId Long merchantId,
                             @SaAccountBookId Long accountBookId) {
        orderPaymentService.delete(String.valueOf(orderPaymentId), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId) {
        return JsonResult.successful(orderPaymentService.load(merchantId, id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        orderPaymentService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/writeOff")
    public JsonResult writeOff(Page page, OrderPaymentService.SupplierQuery query,
                               @SaMerchantId Long merchantId,
                               @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.writeOffCandidates(page, query));
    }
}

