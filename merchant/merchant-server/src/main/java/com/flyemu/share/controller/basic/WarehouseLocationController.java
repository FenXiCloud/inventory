package com.flyemu.share.controller.basic;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.WarehouseLocation;
import com.flyemu.share.service.basic.WarehouseLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/warehouseLocation")
@RequiredArgsConstructor
public class WarehouseLocationController {

    private final WarehouseLocationService warehouseLocationService;

    /**
     * 分页查询
     */
    @GetMapping
    public JsonResult list(Page page, WarehouseLocationService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(warehouseLocationService.query(page, query));
    }

    /**
     * 列表查询（不分页）
     */
    @GetMapping("/list")
    public JsonResult listAll(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(warehouseLocationService.list(
                accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    /**
     * 按仓库查询货位
     */
    @GetMapping("/listByWarehouse")
    public JsonResult listByWarehouse(@RequestParam Long warehouseId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(warehouseLocationService.listByWarehouse(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), warehouseId));
    }

    /**
     * 按类型查询货位
     */
    @GetMapping("/listByType")
    public JsonResult listByType(@RequestParam String type, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(warehouseLocationService.listByType(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), type));
    }

    /**
     * 按仓库和类型查询货位
     */
    @GetMapping("/listByWarehouseAndType")
    public JsonResult listByWarehouseAndType(@RequestParam Long warehouseId, @RequestParam String type,
                                              @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(warehouseLocationService.listByWarehouseAndType(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), warehouseId, type));
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public JsonResult getById(@PathVariable Long id) {
        return JsonResult.successful(warehouseLocationService.getById(id));
    }

    /**
     * 保存
     */
    @PostMapping
    public JsonResult save(@RequestBody @Valid WarehouseLocation warehouseLocation, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(warehouseLocation, accountDto);
        return JsonResult.successful(warehouseLocationService.save(warehouseLocation));
    }

    /**
     * 更新
     */
    @PutMapping
    public JsonResult update(@RequestBody @Valid WarehouseLocation warehouseLocation, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(warehouseLocation, accountDto);
        return JsonResult.successful(warehouseLocationService.save(warehouseLocation));
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public JsonResult delete(@PathVariable Long id, @SaAccountVal AccountDto accountDto) {
        warehouseLocationService.delete(id, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    /**
     * 批量删除
     */
    @PutMapping("/batchDelete")
    public JsonResult batchDelete(@RequestBody List<Long> ids, @SaAccountVal AccountDto accountDto) {
        warehouseLocationService.batchDelete(ids, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }
}
