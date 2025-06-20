package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.controller.PageResults;
import com.flyemu.share.dto.CustomerFlowDTO;
import com.flyemu.share.dto.SupplierFlowDTO;
import com.flyemu.share.entity.fund.SupplierFlow;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.form.CustomerInitialForm;
import com.flyemu.share.form.SupplierInitialForm;
import com.flyemu.share.service.fund.SupplierFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 供货商交易流水/期初
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

    @PostMapping("batchSave")
    public JsonResult batchSave(@RequestBody @Valid SupplierInitialForm form,
                                @SaAccountBookId Long accountBookId,
                                @SaMerchantId Long merchantId,
                                @SaAdminId Long adminId
    ) {
        form.setMerchantId(merchantId);
        form.setAccountBookId(accountBookId);
        form.setCreatedBy(adminId);
        supplierFlowService.batchSave(form);
        return JsonResult.successful();
    }

    /**
     * 详情
     * @param merchantId
     * @param accountBookId
     * @param id
     * @return
     */
    @GetMapping("/getInfo/{id}")
    public JsonResult getInfo(
            @SaMerchantId Long merchantId,
            @SaAccountBookId Long accountBookId,
            @PathVariable Long id
    ) {
        SupplierFlow query = new SupplierFlow();
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        query.setId(id);
        SupplierFlowDTO supplierFlowDTO = supplierFlowService.getById(query);
        return JsonResult.successful(supplierFlowDTO);
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(
            @RequestBody SupplierInitialForm form
    ) {
        supplierFlowService.batchDelete(form);
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
