package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.CustomerFlowDTO;
import com.flyemu.share.dto.InventoryItemDTO;
import com.flyemu.share.entity.fund.CustomerFlow;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.form.CustomerInitialForm;
import com.flyemu.share.form.InventoryInitialForm;
import com.flyemu.share.service.fund.CustomerFlowService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 客户交易流水/期初
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/customerInitial")
@RequiredArgsConstructor
public class CustomerInitialController {

    private final CustomerFlowService customerFlowService;

    @GetMapping
    public JsonResult list(Page page, CustomerFlowService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(customerFlowService.query(page,query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerFlow customerFlow, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        customerFlow.setMerchantId(merchantId);
        customerFlow.setAccountBookId(accountBookId);
        customerFlowService.save(customerFlow);
        return JsonResult.successful();
    }

    @PostMapping("batchSave")
    public JsonResult batchSave(@RequestBody @Valid CustomerInitialForm form,
                                @SaAccountBookId Long accountBookId,
                                @SaMerchantId Long merchantId,
                                @SaAdminId Long adminId
    ) {
        form.setMerchantId(merchantId);
        form.setAccountBookId(accountBookId);
        form.setCreatedBy(adminId);
        customerFlowService.batchSave(form);
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
        InventoryItem query = new InventoryItem();
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        query.setId(id);
        CustomerFlowDTO customerFlowDTO = customerFlowService.getById(query);
        return JsonResult.successful(customerFlowDTO);
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(
            @RequestBody CustomerInitialForm form
    ) {
        customerFlowService.batchDelete(form);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CustomerFlow customerFlow) {
        customerFlowService.save(customerFlow);
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerFlowId}")
    public JsonResult delete(@PathVariable Long customerFlowId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        customerFlowService.delete(customerFlowId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(customerFlowService.select(merchantId, accountBookId));
    }

}
