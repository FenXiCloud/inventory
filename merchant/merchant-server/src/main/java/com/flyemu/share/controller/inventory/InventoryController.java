package com.flyemu.share.controller.inventory;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.Inventory;
import com.flyemu.share.service.inventory.InventoryItemService;
import com.flyemu.share.service.inventory.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    private final InventoryItemService inventoryItemService;

    @GetMapping
    public JsonResult list(Page page, InventoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryService.query(page, query));
    }

    @GetMapping("/products")
    public JsonResult products(@RequestParam(required = false) Long warehouseId, @RequestParam(required = false) Long productId,
                               @RequestParam(required = false) String filter, @RequestParam(required = false) String warehouseIds, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.products(warehouseId, warehouseIds, productId, filter, accountDto.getAccountBookId(), accountDto.getMerchantId()));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Inventory inventory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(inventory, accountDto);
        inventoryService.save(inventory);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Inventory inventory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(inventory, accountDto);
        inventoryService.save(inventory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{inventoryId}")
    public JsonResult delete(@PathVariable Long inventoryId, @SaAccountVal AccountDto accountDto) {
        inventoryService.delete(inventoryId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/selectProduct")
    public JsonResult selectProduct(@RequestParam("warehouseId") Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.selectProduct(accountDto.getMerchantId(), accountDto.getAccountBookId(), warehouseId));
    }

    @GetMapping("/exist/{productId}/{warehouseId}")
    public JsonResult exist(@PathVariable("productId") Long productId, @PathVariable("warehouseId") Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.exist(productId, warehouseId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/totalCost/{productId}/{warehouseId}")
    public JsonResult totalCost(@PathVariable("productId") Long productId, @PathVariable("warehouseId") Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.totalCost(productId, warehouseId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/costDetail/{productId}/{warehouseId}")
    public JsonResult costDetail(@PathVariable("productId") Long productId, @PathVariable("warehouseId") Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.costDetail(productId, warehouseId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/tailDifference")
    public JsonResult tailDifference(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.tailDifference(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/balance")
    public JsonResult balance(Page page, InventoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryService.balance(page, query));
    }

    @GetMapping("/balanceTotal")
    public JsonResult balanceTotal(InventoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryService.balanceTotal(query));
    }

    @GetMapping("/balanceSalesPrice")
    public JsonResult balanceSalesPrice(InventoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryService.productSalesPrices(query));
    }

    @GetMapping("/latestSalesPrices")
    public JsonResult latestSalesPrices(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryService.getLatestSalesPrices(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PostMapping("/rebuildCostChain")
    public JsonResult rebuildCostChain(@RequestParam Long merchantId, @RequestParam Long accountBookId) {
        inventoryItemService.rebuildCostChain(merchantId, accountBookId);
        return JsonResult.successful();
    }

}
