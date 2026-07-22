package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.SupplierCategory;
import com.flyemu.share.service.basic.SupplierCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supplierCategory")
@RequiredArgsConstructor
@SaCheckLogin
public class SupplierCategoryController {

    private final SupplierCategoryService supplierCategoryService;

    @GetMapping
    public JsonResult list(SupplierCategoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(supplierCategoryService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SupplierCategory supplierCategory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplierCategory, accountDto);
        supplierCategoryService.save(supplierCategory);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SupplierCategory supplierCategory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplierCategory, accountDto);
        supplierCategoryService.save(supplierCategory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{supplierCategoryId}")
    public JsonResult delete(@PathVariable Long supplierCategoryId, @SaAccountVal AccountDto accountDto) {
        supplierCategoryService.delete(supplierCategoryId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(supplierCategoryService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
