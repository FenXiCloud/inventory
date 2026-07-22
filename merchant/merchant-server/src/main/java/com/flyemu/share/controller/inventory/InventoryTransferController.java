package com.flyemu.share.controller.inventory;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.inventory.InventoryTransfer;
import com.flyemu.share.enums.OrderStatus;
import com.flyemu.share.form.InventoryTransferForm;
import com.flyemu.share.service.inventory.InventoryTransferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventoryTransfer")
@RequiredArgsConstructor
public class InventoryTransferController {

    private final InventoryTransferService inventoryTransferService;

    @GetMapping
    public JsonResult list(Page page, InventoryTransferService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(inventoryTransferService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid InventoryTransferForm inventoryTransferForm,
                           @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        InventoryTransfer inventoryTransfer = inventoryTransferForm.getInventoryTransfer();
        TenantScope.bind(inventoryTransfer, accountDto);
        inventoryTransfer.setOrderStatus(OrderStatus.已保存);
        inventoryTransfer.setCreatedBy(adminId);
        return JsonResult.successful(inventoryTransferService.save(inventoryTransferForm, accountDto.getMerchantId()));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid InventoryTransferForm inventoryTransferForm, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryTransferService.save(inventoryTransferForm, accountDto.getMerchantId()));
    }

    @DeleteMapping("/{inventoryTransferId}")
    public JsonResult delete(@PathVariable Long inventoryTransferId, @SaAccountVal AccountDto accountDto) {
        inventoryTransferService.delete(inventoryTransferId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryTransferService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/load/{id}")
    public JsonResult load(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(inventoryTransferService.load(accountDto.getMerchantId(), id));
    }

    @PostMapping("/approved/{state}")
    public JsonResult approved(@RequestBody List<Long> ids, @PathVariable OrderStatus state, @SaAccountVal AccountDto accountDto) {
        inventoryTransferService.approved(ids, state, accountDto.getAdminId(), accountDto.getMerchantId());
        return JsonResult.successful();
    }
}
