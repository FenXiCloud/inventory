package com.flyemu.share.controller.purchase;


import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.service.purchase.PurchaseReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * @功能描述: 采购订单报表
 * @创建时间: 2025年02月23日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/purchaseReport")
@RequiredArgsConstructor
public class PurchaseReportController {


    private final PurchaseReportService purchaseReportService;

    @GetMapping
    public JsonResult list(Page page, PurchaseReportService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseReportService.query(page, query));
    }

    @GetMapping("/stat")
    public JsonResult listStat(Page page, PurchaseReportService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {

        Set<String> groupValuesSet = Set.of(query.groupValues.toArray(new String[0]));
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(purchaseReportService.queryStat(page, query,groupValuesSet));
    }
}
