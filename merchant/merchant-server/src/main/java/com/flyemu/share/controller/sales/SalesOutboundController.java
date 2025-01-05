package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.service.sales.SalesOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 销售出库单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/salesOutbound")
@RequiredArgsConstructor
public class SalesOutboundController {

    private final SalesOutboundService salesOutboundService;

    @GetMapping
    public JsonResult list(Page page, SalesOutboundService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesOutboundService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SalesOutbound salesOutbound, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        salesOutbound.setMerchantId(merchantId);
        salesOutbound.setAccountBookId(accountBookId);
        salesOutboundService.save(salesOutbound);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOutbound salesOutbound) {
        salesOutboundService.save(salesOutbound);
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOutboundId}")
    public JsonResult delete(@PathVariable Long salesOutboundId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        salesOutboundService.delete(salesOutboundId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(salesOutboundService.select(merchantId, accountBookId));
    }

}
