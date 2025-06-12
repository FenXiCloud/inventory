package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.service.fund.AccountFlowService;
import com.flyemu.share.service.fund.OrderPaymentService;
import com.flyemu.share.service.fund.OrderReceiptService;
import com.flyemu.share.dto.OtherFundDetailsVO;
import com.flyemu.share.service.fund.vo.report.SummaryReceivableDetailsQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @author shuaiqi
 * @功能描述: 资金明细
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/accountFlow")
@RequiredArgsConstructor
public class AccountFlowController {

    private final AccountFlowService accountFlowService;
    private final OrderPaymentService orderPaymentService;
    private final OrderReceiptService orderReceiptService;

    //现金流水
    @GetMapping("list")
    public JsonResult list(Page page, AccountFlowService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountFlowService.query(page, query));
    }

    // 供应商应付明细
    @GetMapping("getPayableDetailReport")
    public JsonResult getPayableDetailReport(Page page, OrderPaymentService.PayableDetailReportQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.getPayableDetailReport(page, query));
    }

    // 客户应收明细
    @GetMapping("getReceivableDetailReport")
    public JsonResult getReceivableDetailReport(Page page, OrderReceiptService.ReceivableDetailReportQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.getReceivableDetailReport(page, query));
    }

    //应付汇总明细
    @GetMapping("summaryPayableDetails")
    public JsonResult summaryPayableDetails(Page page, OrderPaymentService.SummaryPayableDetailsQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderPaymentService.summaryPayableDetails(page, query));
    }

    //应收汇总明细
    @GetMapping("summaryReceivableDetails")
    public JsonResult summaryReceivableDetails(Page page, SummaryReceivableDetailsQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(orderReceiptService.summaryReceivableDetails(page, query));
    }
    //其他收支明细
    @GetMapping("/otherFundDetails")
    public JsonResult getOtherFundDetails(Page page, @ModelAttribute AccountFlowService.OtherFundQuery query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(accountFlowService.queryOtherFundDetails(page, query));
    }

}
