package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.inventory.InventoryItem;
import com.flyemu.share.form.InventoryInitialForm;
import com.flyemu.share.service.inventory.InventoryItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventoryInitial")
@RequiredArgsConstructor
public class InventoryInitialController {

    private final InventoryItemService inventoryItemService;

    @GetMapping
    public JsonResult list(Page page, InventoryItemService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryItemService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryItem inventoryItem, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(inventoryItem, accountDto);
        inventoryItemService.save(inventoryItem);
        return JsonResult.successful();
    }

    @PostMapping("/batch")
    public JsonResult batch(@RequestBody @Valid InventoryInitialForm inventoryInitialForm,
                            @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(inventoryInitialForm, accountDto);
        inventoryInitialForm.setCreatedBy(adminId);
        inventoryItemService.batch(inventoryInitialForm);
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

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        InventoryItem query = new InventoryItem();
        TenantScope.bind(query, accountDto);
        query.setId(id);
        return JsonResult.successful(inventoryItemService.getById(query));
    }

    @PutMapping("/batchDelete")
    public JsonResult batchDelete(@RequestBody InventoryInitialForm inventoryInitialForm, @SaAccountVal AccountDto accountDto) {
        inventoryItemService.batchDelete(inventoryInitialForm.getIds(), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

}
