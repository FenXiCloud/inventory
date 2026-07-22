package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.service.basic.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
@SaCheckLogin
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public JsonResult list(Page page, SupplierService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(supplierService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Supplier supplier, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplier, accountDto);
        supplierService.save(supplier);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Supplier supplier, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(supplier, accountDto);
        supplierService.save(supplier);
        return JsonResult.successful();
    }

    @DeleteMapping("/{supplierId}")
    public JsonResult delete(@PathVariable Long supplierId, @SaAccountVal AccountDto accountDto) {
        supplierService.delete(supplierId, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(supplierService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/product/select/{supplierId}")
    public JsonResult select(@PathVariable Long supplierId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(supplierService.selectProducts(supplierId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
