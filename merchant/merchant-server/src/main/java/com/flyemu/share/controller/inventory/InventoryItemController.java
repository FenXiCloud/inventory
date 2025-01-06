package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.service.inventory.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 库存明细表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/inventoryItem")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    @GetMapping
    public JsonResult list(InventoryItemService.Query query, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryItem inventoryItem, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        inventoryItem.setMerchantId(merchantId);
        inventoryItem.setAccountBookId(accountBookId);
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid InventoryItem inventoryItem) {
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @DeleteMapping("/{inventoryItemId}")
    public JsonResult delete(@PathVariable Long inventoryItemId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        inventoryItemService.delete(inventoryItemId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryItemService.select(merchantId, accountBookId));
    }

}
