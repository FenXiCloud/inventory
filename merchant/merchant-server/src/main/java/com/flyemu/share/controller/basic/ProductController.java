package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.service.basic.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@SaCheckLogin
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public JsonResult list(Page page, ProductService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(productService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid ProductForm productForm, @SaAccountVal AccountDto accountDto) {
        Assert.isNull(productForm.getProduct().getId(), "新增商品Id必须为空~");
        productService.save(productForm, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid ProductForm productForm, @SaAccountVal AccountDto accountDto) {
        Product product = productForm.getProduct();
        Assert.notNull(product.getId(), "更新商品Id不允许为空~");
        if (productForm.getCustomerLevelPriceList() == null && product.getEnabled() != null) {
            productService.updateById(product, accountDto.getMerchantId(), accountDto.getAccountBookId());
        } else {
            productService.save(productForm, accountDto.getMerchantId(), accountDto.getAccountBookId());
        }
        return JsonResult.successful();
    }

    @GetMapping("/load/{productId}")
    public JsonResult load(@PathVariable Long productId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productService.loadById(productId, accountDto.getMerchantId()));
    }

    @DeleteMapping("/{productId}")
    public JsonResult delete(@PathVariable Long productId, @SaAccountVal AccountDto accountDto) {
        productService.delete(productId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(Long productCategoryId, Long warehouseId, Long customerId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productService.select(accountDto.getMerchantId(), accountDto.getAccountBookId(), productCategoryId, warehouseId, customerId));
    }

    @GetMapping("/customerLevel/price/{productId}")
    public JsonResult customerLevelPrice(@PathVariable Long productId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productService.customerLevelPrice(productId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
