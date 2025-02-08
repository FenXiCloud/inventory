package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.sales.SalesReturn;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.form.SalesReturnForm;
import com.flyemu.share.service.sales.SalesReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * @功能描述: 销售退货单
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/salesReturn")
@RequiredArgsConstructor
public class SalesReturnController {

    private final SalesReturnService salesReturnService;

    @GetMapping
    public JsonResult list(Page page, SalesReturnService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesReturnService.query(page, query));
    }

    @PostMapping
    public JsonResult save(
        @RequestBody @Valid SalesReturnForm salesReturnForm,
        @SaAccountBookId Long accountBookId,
        @SaMerchantId Long merchantId,
        @SaAdminId Long adminId
    ) {
        salesReturnForm.getSalesReturn().setMerchantId(merchantId);
        salesReturnForm.getSalesReturn().setAccountBookId(accountBookId);
        salesReturnForm.getSalesReturn().setCreatedBy(adminId);
        salesReturnForm.getSalesReturn().setCreatedAt(LocalDateTime.now());
        salesReturnService.save(salesReturnForm);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update( @RequestBody @Valid SalesReturnForm salesReturnForm) {
        salesReturnService.save(salesReturnForm);
        return JsonResult.successful();
    }

    @DeleteMapping("/{salesReturnId}")
    public JsonResult delete(@PathVariable Long salesReturnId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        salesReturnService.delete(salesReturnId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(salesReturnService.select(merchantId, accountBookId));
    }

    /**
     * 销售退货单详情
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
        SalesReturn query = new SalesReturn();
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        query.setId(orderId);
        return JsonResult.successful(salesReturnService.getById(query));
    }

    @PutMapping("/batchAudit")
    public JsonResult batchAudit(
            @RequestBody SalesReturnForm salesReturnForm,
            @SaAdminId Long adminId
    ) {
        salesReturnForm.setSalesReturn(new SalesReturn());
        salesReturnForm.getSalesReturn().setApprovedBy(adminId);
        salesReturnService.batchAudit(salesReturnForm);
        return JsonResult.successful();
    }

}
