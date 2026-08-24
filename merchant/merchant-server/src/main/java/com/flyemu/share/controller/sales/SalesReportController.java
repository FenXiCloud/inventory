package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.form.SalesReportForm;
import com.flyemu.share.service.sales.SalesReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 销售报表
 * 路径约定：/salesReport/{item|summary|profit|ranking}
 */
@RestController
@RequestMapping("/salesReport")
@RequiredArgsConstructor
public class SalesReportController {

    private final SalesReportService salesReportService;

    @PostMapping("/item")
    public JsonResult item(Page page, @RequestBody(required = false) SalesReportForm salesReportForm, @SaAccountVal AccountDto accountDto) {
        salesReportForm = ensureForm(salesReportForm, accountDto);
        return JsonResult.successful(salesReportService.item(page, salesReportForm));
    }

    @PostMapping("/summary")
    public JsonResult summary(Page page, @RequestBody(required = false) SalesReportForm salesReportForm, @SaAccountVal AccountDto accountDto) {
        salesReportForm = ensureForm(salesReportForm, accountDto);
        return JsonResult.successful(salesReportService.summary(page, salesReportForm));
    }

    @PostMapping("/profit")
    public JsonResult profit(Page page, @RequestBody(required = false) SalesReportForm salesReportForm, @SaAccountVal AccountDto accountDto) {
        salesReportForm = ensureForm(salesReportForm, accountDto);
        return JsonResult.successful(salesReportService.profit(page, salesReportForm));
    }

    @PostMapping("/ranking")
    public JsonResult ranking(Page page, @RequestBody(required = false) SalesReportForm salesReportForm, @SaAccountVal AccountDto accountDto) {
        salesReportForm = ensureForm(salesReportForm, accountDto);
        return JsonResult.successful(salesReportService.ranking(page, salesReportForm));
    }

    @PostMapping("/statistics")
    public JsonResult statistics(@RequestBody(required = false) SalesReportForm salesReportForm, @SaAccountVal AccountDto accountDto) {
        salesReportForm = ensureForm(salesReportForm, accountDto);
        return JsonResult.successful(salesReportService.statistics(salesReportForm));
    }

    private SalesReportForm ensureForm(SalesReportForm form, AccountDto accountDto) {
        if (form == null) {
            form = new SalesReportForm();
        }
        TenantScope.bind(form, accountDto);
        return form;
    }
}
