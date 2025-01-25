package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.entity.sales.SalesOutbound;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.service.sales.SalesOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

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
    public JsonResult save(
            @RequestBody @Valid SalesOutboundForm salesOutboundForm,
            @SaAccountBookId Long accountBookId,
            @SaMerchantId Long merchantId,
            @SaAdminId Long adminId
    ) {
        salesOutboundForm.getSalesOutbound().setMerchantId(merchantId);
        salesOutboundForm.getSalesOutbound().setAccountBookId(accountBookId);
        salesOutboundForm.getSalesOutbound().setCreatedBy(adminId);
        salesOutboundForm.getSalesOutbound().setCreatedAt(LocalDateTime.now());
        salesOutboundService.save(salesOutboundForm);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOutboundForm salesOutboundForm) {
        salesOutboundService.save(salesOutboundForm);
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

    /**
     * 销售出库订单详情
     * @param merchantId
     * @param accountBookId
     * @param orderId
     * @return
     */
    @GetMapping("/getInfo/{orderId}")
    public JsonResult getInfo(
            @SaMerchantId Long merchantId,
            @SaAccountBookId Long accountBookId,
            @PathVariable Long orderId
    ) {
        SalesOrder query = new SalesOrder();
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        query.setId(orderId);
        return JsonResult.successful(salesOutboundService.getById(query));
    }

    @PutMapping("/batchAudit")
    public JsonResult batchAudit(
            @RequestBody SalesOutboundForm salesOutboundForm,
            @SaAdminId Long adminId
    ) {
        salesOutboundForm.setSalesOutbound(new SalesOutbound());
        salesOutboundForm.getSalesOutbound().setApprovedBy(adminId);
        salesOutboundService.batchAudit(salesOutboundForm);
        return JsonResult.successful();
    }

}
