package com.flyemu.share.controller;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Warehouse;
import com.flyemu.share.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public JsonResult list(WarehouseService.Query query, @SaAccountVal AccountDto accountDto) {
        query.setAccountBookId(accountDto.getAccountBookId());
        query.setMerchantId(accountDto.getMerchantId());
        return JsonResult.successful(warehouseService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Warehouse warehouse, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        warehouse.setMerchantId(merchantId);
        warehouse.setAccountBookId(accountBookId);
        warehouseService.save(warehouse);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Warehouse warehouse, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        warehouse.setMerchantId(merchantId);
        warehouse.setAccountBookId(accountBookId);
        warehouseService.save(warehouse);
        return JsonResult.successful();
    }

    @DeleteMapping("/{warehouseId}")
    public JsonResult delete(@PathVariable Long warehouseId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        warehouseService.delete(warehouseId,merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(warehouseService.select(accountDto.getMerchantId(),accountDto.getAccountBookId()));
    }
}
