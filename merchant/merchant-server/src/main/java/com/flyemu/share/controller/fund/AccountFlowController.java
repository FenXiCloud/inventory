package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.service.fund.AccountFlowService;
import com.flyemu.share.service.fund.OrderPaymentService;
import com.flyemu.share.service.fund.OrderReceiptService;
import com.flyemu.share.service.fund.vo.report.SummaryReceivableDetailsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 资金流水 / 资金报表
 */
@RestController
@RequestMapping("/accountFlow")
@RequiredArgsConstructor
public class AccountFlowController {

    private final AccountFlowService accountFlowService;
    private final OrderPaymentService orderPaymentService;
    private final OrderReceiptService orderReceiptService;

    @GetMapping
    public JsonResult list(Page page, AccountFlowService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountFlowService.query(page, query));
    }

    @GetMapping("payableDetail")
    public JsonResult payableDetail(Page page, OrderPaymentService.PayableDetailReportQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.payableDetail(page, query));
    }

    @GetMapping("receivableDetail")
    public JsonResult receivableDetail(Page page, OrderReceiptService.ReceivableDetailReportQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.receivableDetail(page, query));
    }

    @GetMapping("summaryPayable")
    public JsonResult summaryPayable(Page page, OrderPaymentService.SummaryPayableDetailsQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.summaryPayableDetails(page, query));
    }

    @GetMapping("summaryReceivable")
    public JsonResult summaryReceivable(Page page, SummaryReceivableDetailsQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.summaryReceivableDetails(page, query));
    }

    @GetMapping("otherFund")
    public JsonResult otherFund(Page page, @ModelAttribute AccountFlowService.OtherFundQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountFlowService.queryOtherFundDetails(page, query));
    }

}
