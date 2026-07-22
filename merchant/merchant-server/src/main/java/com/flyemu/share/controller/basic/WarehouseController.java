package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Warehouse;
import com.flyemu.share.service.basic.WarehouseService;
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
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(warehouseService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Warehouse warehouse, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(warehouse, accountDto);
        warehouseService.save(warehouse);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Warehouse warehouse, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(warehouse, accountDto);
        warehouseService.save(warehouse);
        return JsonResult.successful();
    }

    @DeleteMapping("/{warehouseId}")
    public JsonResult delete(@PathVariable Long warehouseId, @SaAccountVal AccountDto accountDto) {
        warehouseService.delete(warehouseId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(warehouseService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
