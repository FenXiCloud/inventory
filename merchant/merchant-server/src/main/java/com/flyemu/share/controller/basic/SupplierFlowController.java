package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.SupplierFlow;
import com.flyemu.share.service.basic.SupplierFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 供货商交易流水/期初
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/supplierFlow")
@RequiredArgsConstructor
public class SupplierFlowController {

    private final SupplierFlowService supplierFlowService;

    @GetMapping
    public JsonResult list(SupplierFlowService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(supplierFlowService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SupplierFlow supplierFlow, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        supplierFlow.setMerchantId(merchantId);
        supplierFlow.setAccountBookId(accountBookId);
        supplierFlowService.save(supplierFlow);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SupplierFlow supplierFlow) {
        supplierFlowService.save(supplierFlow);
        return JsonResult.successful();
    }

    @DeleteMapping("/{supplierFlowId}")
    public JsonResult delete(@PathVariable Long supplierFlowId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        supplierFlowService.delete(supplierFlowId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(supplierFlowService.select(merchantId, accountBookId));
    }

}
