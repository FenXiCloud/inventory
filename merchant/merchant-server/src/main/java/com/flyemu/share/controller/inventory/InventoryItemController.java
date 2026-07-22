package com.flyemu.share.controller.inventory;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
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
    public JsonResult list(InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryItem inventoryItem, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(inventoryItem, accountDto);
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid InventoryItem inventoryItem, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(inventoryItem, accountDto);
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @DeleteMapping("/{inventoryItemId}")
    public JsonResult delete(@PathVariable Long inventoryItemId, @SaAccountVal AccountDto accountDto) {
        inventoryItemService.delete(inventoryItemId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryItemService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/item")
    public JsonResult item(Page page, InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.report(page, query));
    }

    @GetMapping("/itemTotal")
    public JsonResult itemTotal(InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.detailTotal(query));
    }

    @GetMapping("/summary")
    public JsonResult summary(Page page, InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.summary(page, query));
    }

    @GetMapping("/summaryByType")
    public JsonResult summaryByType(InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.summaryOperationType(query));
    }

    @GetMapping("/summaryInitial")
    public JsonResult summaryInitial(InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.summaryInitial(query));
    }

    @GetMapping("/balance")
    public JsonResult balance(Page page, InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.balance(page, query));
    }
}

