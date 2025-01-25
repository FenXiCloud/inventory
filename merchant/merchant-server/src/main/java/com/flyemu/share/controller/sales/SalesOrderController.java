package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.sales.SalesOrder;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.service.sales.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @功能描述: 销售订单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/salesOrder")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @GetMapping
    public JsonResult list(Page page, SalesOrderService.Query query,
                           @SaAccountBookId Long accountBookId,
                           @SaMerchantId Long merchantId
    ) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesOrderService.query(page, query));
    }

    @PostMapping
    public JsonResult save(
            @RequestBody @Valid SalesOrderForm salesOrderForm,
            @SaAccountBookId Long accountBookId,
            @SaMerchantId Long merchantId,
            @SaAdminId Long adminId
    ) {
        salesOrderForm.getSalesOrder().setMerchantId(merchantId);
        salesOrderForm.getSalesOrder().setAccountBookId(accountBookId);
        salesOrderForm.getSalesOrder().setCreatedBy(adminId);
        salesOrderForm.getSalesOrder().setCreatedAt(LocalDateTime.now());
        salesOrderService.save(salesOrderForm);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOrderForm salesOrderForm) {
        salesOrderService.save(salesOrderForm);
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOrderId}")
    public JsonResult delete(@PathVariable Long salesOrderId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        salesOrderService.delete(salesOrderId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(salesOrderService.select(merchantId, accountBookId));
    }

    /**
     * 销售订单详情
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
        return JsonResult.successful(salesOrderService.getById(query));
    }

    @PutMapping("/batchAudit")
    public JsonResult batchAudit(@RequestBody SalesOrderForm salesOrderForm) {
        salesOrderService.batchAudit(salesOrderForm);
        return JsonResult.successful();
    }

}
