package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.entity.basic.ProductCategory;
import com.flyemu.share.service.basic.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequestMapping("/productCategory")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    public JsonResult list(ProductCategoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(productCategoryService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid ProductCategory productCategory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(productCategory, accountDto);
        return JsonResult.successful(productCategoryService.save(productCategory));
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid ProductCategory productCategory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(productCategory, accountDto);
        return JsonResult.successful(productCategoryService.save(productCategory));
    }

    @DeleteMapping("/{productCategoryId}")
    public JsonResult delete(@PathVariable Long productCategoryId, @SaAccountVal AccountDto accountDto) {
        productCategoryService.delete(accountDto.getMerchantId(), productCategoryId, accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productCategoryService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

}
