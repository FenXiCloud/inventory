package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.form.SalesReportForm;
import com.flyemu.share.service.sales.SalesOrderService;
import com.flyemu.share.service.sales.SalesReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @功能描述: 销售报表
 * @作者: wl
 */
@RestController
@RequestMapping("/salesReport")
@RequiredArgsConstructor
public class SalesReportController {

    private final SalesReportService salesReportService;

    /**
     * 销售明细报表
     *
     * @param page
     * @param salesReportForm
     * @param accountBookId
     * @param merchantId
     * @return
     */
    @GetMapping("/salesItem")
    public JsonResult salesItem(
        Page page,
        SalesReportForm salesReportForm,
        @SaAccountBookId Long accountBookId,
        @SaMerchantId Long merchantId
    ) {
        salesReportForm.setMerchantId(merchantId);
        salesReportForm.setAccountBookId(accountBookId);
        return JsonResult.successful(salesReportService.salesItem(page, salesReportForm));
    }

    /**
     * 销售汇总报表
     *
     * @param page
     * @param salesReportForm
     * @param accountBookId
     * @param merchantId
     * @return
     */
    @GetMapping("/salesSummary")
    public JsonResult salesSummary(
            Page page,
            SalesReportForm salesReportForm,
            @SaAccountBookId Long accountBookId,
            @SaMerchantId Long merchantId
    ) {
        salesReportForm.setMerchantId(merchantId);
        salesReportForm.setAccountBookId(accountBookId);
        return JsonResult.successful(salesReportService.salesSummary(page, salesReportForm));
    }

}
