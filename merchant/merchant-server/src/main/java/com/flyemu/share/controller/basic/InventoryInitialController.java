package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.form.InventoryInitialForm;
import com.flyemu.share.service.inventory.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 库存期初
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/inventoryInitial")
@RequiredArgsConstructor
public class InventoryInitialController {

    private final InventoryItemService inventoryItemService;

    @GetMapping
    public JsonResult list(Page page, InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryItem inventoryItem, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventoryItem.setMerchantId(merchantId);
        inventoryItem.setAccountBookId(accountBookId);
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @PostMapping("/batch")
    public JsonResult batch(@RequestBody @Valid InventoryInitialForm inventoryInitialForm,
                            @SaMerchantId Long merchantId,
                            @SaAccountBookId Long accountBookId,
                            @SaAdminId Long adminId) {
        inventoryInitialForm.setMerchantId(merchantId);
        inventoryInitialForm.setAccountBookId(accountBookId);
        inventoryInitialForm.setCreatedBy(adminId);
        inventoryItemService.batch(inventoryInitialForm);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid InventoryItem inventoryItem, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventoryItem.setMerchantId(merchantId);
        inventoryItem.setAccountBookId(accountBookId);
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @DeleteMapping("/{inventoryItemId}")
    public JsonResult delete(@PathVariable Long inventoryItemId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventoryItemService.delete(inventoryItemId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryItemService.select(merchantId, accountBookId));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        InventoryItem query = new InventoryItem();
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        query.setId(id);
        return JsonResult.successful(inventoryItemService.getById(query));
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(@RequestBody InventoryInitialForm inventoryInitialForm,
                                  @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventoryItemService.batchDelete(inventoryInitialForm.getIds(), merchantId, accountBookId);
        return JsonResult.successful();
    }

}
