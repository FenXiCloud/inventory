package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.form.SupplierInitialForm;
import com.flyemu.share.service.fund.SupplierFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 货商期初
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/supplierInitial")
@RequiredArgsConstructor
public class SupplierInitialController {

    private final SupplierFlowService supplierFlowService;

    @GetMapping
    public JsonResult list(Page page, SupplierFlowService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(supplierFlowService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SupplierFlow supplierFlow, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        supplierFlow.setMerchantId(merchantId);
        supplierFlow.setAccountBookId(accountBookId);
        supplierFlowService.save(supplierFlow);
        return JsonResult.successful();
    }

    @PostMapping("batch")
    public JsonResult batch(@RequestBody @Valid SupplierInitialForm form,
                                @SaAccountBookId Long accountBookId,
                                @SaMerchantId Long merchantId,
                                @SaAdminId Long adminId) {
        form.setMerchantId(merchantId);
        form.setAccountBookId(accountBookId);
        form.setCreatedBy(adminId);
        supplierFlowService.batch(form);
        return JsonResult.successful();
    }

    @GetMapping("load/{id}")
    public JsonResult load(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId, @PathVariable Long id) {
        SupplierFlow query = new SupplierFlow();
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        query.setId(id);
        return JsonResult.successful(supplierFlowService.getById(query));
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(@RequestBody SupplierInitialForm form,
                                  @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        supplierFlowService.batchDelete(form.getIds(), merchantId, accountBookId);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SupplierFlow supplierFlow, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        supplierFlow.setMerchantId(merchantId);
        supplierFlow.setAccountBookId(accountBookId);
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
