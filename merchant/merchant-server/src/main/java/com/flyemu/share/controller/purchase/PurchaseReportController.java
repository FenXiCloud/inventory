package com.flyemu.share.controller.purchase;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.service.purchase.PurchaseReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/purchaseReport")
@RequiredArgsConstructor
public class PurchaseReportController {

    private final PurchaseReportService purchaseReportService;

    @GetMapping
    public JsonResult list(Page page, PurchaseReportService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReportService.query(page, query));
    }

    @GetMapping("/summary")
    public JsonResult summary(Page page, PurchaseReportService.Query query, @SaAccountVal AccountDto accountDto) {
        if (query.groupValues == null || query.groupValues.isEmpty()) {
            return JsonResult.failure("请选择统计字段");
        }
        Set<String> groupValuesSet = Set.copyOf(query.groupValues);
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReportService.summary(page, query, groupValuesSet));
    }

    @GetMapping("/statistics")
    public JsonResult statistics(PurchaseReportService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(purchaseReportService.statistics(query));
    }
}
