package com.flyemu.share.controller.sales;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.SalesReturnForm;
import com.flyemu.share.service.sales.SalesReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

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

    @GetMapping("/total")
    public JsonResult queryTotal(SalesReturnService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(salesReturnService.queryTotal(query));
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
        salesReturnForm.getSalesReturn().setOrderStatus(OrderStatus.已保存);
        salesReturnService.save(salesReturnForm, merchantId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SalesReturnForm salesReturnForm, @SaMerchantId Long merchantId) {
        salesReturnService.save(salesReturnForm, merchantId);
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

    @GetMapping("load/{orderId}")
    public JsonResult load(@SaMerchantId Long merchantId, @PathVariable Long orderId) {
        return JsonResult.successful(salesReturnService.load(merchantId, orderId));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        salesReturnService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }

}
