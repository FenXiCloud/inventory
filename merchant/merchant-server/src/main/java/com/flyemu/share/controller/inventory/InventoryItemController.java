package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.service.inventory.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventoryItem")
@RequiredArgsConstructor
public class InventoryItemController {

    private final InventoryItemService inventoryItemService;

    @GetMapping
    public JsonResult list(InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryItem inventoryItem, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        inventoryItem.setMerchantId(merchantId);
        inventoryItem.setAccountBookId(accountBookId);
        inventoryItemService.save(inventoryItem);
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

    @GetMapping("/item")
    public JsonResult item(Page page, InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.report(page, query));
    }

    @GetMapping("/itemTotal")
    public JsonResult itemTotal(InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.detailTotal(query));
    }

    @GetMapping("/summary")
    public JsonResult summary(Page page, InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.summary(page, query));
    }

    @GetMapping("/summaryByType")
    public JsonResult summaryByType(InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.summaryOperationType(query));
    }

    @GetMapping("/summaryInitial")
    public JsonResult summaryInitial(InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.summaryInitial(query));
    }

    @GetMapping("/balance")
    public JsonResult balance(Page page, InventoryItemService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(inventoryItemService.balance(page, query));
    }
}

