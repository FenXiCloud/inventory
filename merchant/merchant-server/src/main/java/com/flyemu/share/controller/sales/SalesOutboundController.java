package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesOutboundForm;
import com.flyemu.share.service.sales.SalesOutboundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @GetMapping("/total")
    public JsonResult queryTotal(SalesOutboundService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesOutboundService.queryTotal(query));
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
        salesOutboundForm.getSalesOutbound().setOrderStatus(OrderStatus.已保存);
        salesOutboundService.save(salesOutboundForm, merchantId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesOutboundForm salesOutboundForm, @SaMerchantId Long merchantId) {
        salesOutboundService.save(salesOutboundForm, merchantId);
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

    @GetMapping("load/{orderId}")
    public JsonResult load(@SaMerchantId Long merchantId, @PathVariable Long orderId) {
        return JsonResult.successful(salesOutboundService.load(merchantId, orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesOutboundService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
