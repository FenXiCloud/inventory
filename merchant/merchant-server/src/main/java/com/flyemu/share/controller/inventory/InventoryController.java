package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.service.inventory.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 库存余额表
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public JsonResult list(Page page, InventoryService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryService.query(page, query));
    }

    @GetMapping("/products")
    public JsonResult products(@RequestParam(required = false) Long warehouseId, @RequestParam(required = false) Long productId,
                               @RequestParam(required = false) String filter, @RequestParam(required = false) String warehouseIds,
                               @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryService.products(warehouseId, warehouseIds, productId, filter, accountBookId, merchantId));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Inventory inventory, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventory.setMerchantId(merchantId);
        inventory.setAccountBookId(accountBookId);
        inventoryService.save(inventory);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Inventory inventory, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventory.setMerchantId(merchantId);
        inventory.setAccountBookId(accountBookId);
        inventoryService.save(inventory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{inventoryId}")
    public JsonResult delete(@PathVariable Long inventoryId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventoryService.delete(inventoryId, merchantId, accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryService.select(merchantId, accountBookId));
    }

    @GetMapping("/selectProduct")
    public JsonResult selectProduct(@RequestParam("warehouseId") Long warehouseId, @SaMerchantId Long merchantId,
                                    @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryService.selectProduct(merchantId, accountBookId, warehouseId));
    }

    @GetMapping("/exist/{productId}/{warehouseId}")
    public JsonResult exist(@PathVariable("productId") Long productId, @PathVariable("warehouseId") Long warehouseId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryService.exist(productId, warehouseId, merchantId, accountBookId));
    }

    @GetMapping("/totalCost/{productId}/{warehouseId}")
    public JsonResult totalCost(@PathVariable("productId") Long productId, @PathVariable("warehouseId") Long warehouseId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(inventoryService.totalCost(productId, warehouseId, merchantId, accountBookId));
    }

    @GetMapping("/balance")
    public JsonResult balance(Page page, InventoryService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryService.balance(page, query));
    }

    @GetMapping("/balanceTotal")
    public JsonResult balanceTotal(InventoryService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryService.balanceTotal(query));
    }

}
