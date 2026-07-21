package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOrderForm;
import com.flyemu.share.service.sales.SalesOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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
    public JsonResult list(Page page, SalesOrderService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesOrderService.query(page, query));
    }

    @GetMapping("/total")
    public JsonResult queryTotal(SalesOrderService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesOrderService.queryTotal(query));
    }

    @PostMapping
    public JsonResult save(
            @RequestBody @Valid SalesOrderForm salesOrderForm,
            @SaMerchantId Long merchantId,
            @SaAccountBookId Long accountBookId,
            @SaAdminId Long adminId
    ) {
        salesOrderForm.getSalesOrder().setMerchantId(merchantId);
        salesOrderForm.getSalesOrder().setAccountBookId(accountBookId);
        salesOrderForm.getSalesOrder().setCreatedBy(adminId);
        salesOrderForm.getSalesOrder().setCreatedAt(LocalDateTime.now());
        salesOrderForm.getSalesOrder().setOrderStatus(OrderStatus.已保存);
        salesOrderService.save(salesOrderForm, merchantId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOrderForm salesOrderForm, @SaMerchantId Long merchantId) {
        salesOrderService.save(salesOrderForm, merchantId);
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesOrderId}")
    public JsonResult delete(@PathVariable Long salesOrderId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        salesOrderService.delete(salesOrderId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(salesOrderService.select(merchantId, accountBookId));
    }

    @GetMapping("/load/{orderId}")
    public JsonResult load(@PathVariable Long orderId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(salesOrderService.load(merchantId, orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesOrderService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
