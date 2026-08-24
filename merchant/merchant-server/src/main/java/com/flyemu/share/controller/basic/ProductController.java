package com.flyemu.share.controller.basic;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.common.ImportVoUtil;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.ProductImportVo;
import com.flyemu.share.entity.basic.Product;
import com.flyemu.share.exception.ServiceException;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.service.basic.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@SaCheckLogin
@Slf4j
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

    @PostMapping("/quickCreate")
    public JsonResult quickCreate(@RequestBody Map<String, Object> body, @SaAccountVal AccountDto accountDto) {
        String name = (String) body.get("name");
        String unitName = (String) body.get("unitName");
        Long productCategoryId = null;
        Object categoryId = body.get("productCategoryId");
        if (categoryId != null && !String.valueOf(categoryId).trim().isEmpty()) {
            try {
                productCategoryId = Long.valueOf(String.valueOf(categoryId).trim());
            } catch (NumberFormatException ignored) {
                // 类别ID非数字（前端直接传入了名称）时，视为未选择类别
            }
        }
        return JsonResult.successful(productService.quickCreate(name, unitName, productCategoryId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @GetMapping("/customerLevel/price/{productId}")
    public JsonResult customerLevelPrice(@PathVariable Long productId, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productService.customerLevelPrice(productId, accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    // ===== 品牌管理 =====
    @GetMapping("/brands")
    public JsonResult brands(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(productService.brandList(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }

    @PutMapping("/brand")
    public JsonResult renameBrand(@RequestBody Map<String, String> body, @SaAccountVal AccountDto accountDto) {
        productService.renameBrand(body.get("oldBrand"), body.get("newBrand"), accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @DeleteMapping("/brand")
    public JsonResult deleteBrand(@RequestParam String brand, @SaAccountVal AccountDto accountDto) {
        productService.deleteBrand(brand, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    // 批量导入
    @PostMapping("/importData")
    public JsonResult importData(@RequestParam("file") MultipartFile multipartFile, @SaAccountVal AccountDto accountDto) {
        try {
            List<ProductImportVo> rows = ImportVoUtil.readImportFile(multipartFile, ProductImportVo.class);
            if (CollUtil.isEmpty(rows)) {
                throw new ServiceException("excel中未解析到可以导入的数据");
            }
            if (rows.size() > 1000) {
                throw new ServiceException("导入数据不能大于1000行");
            }
            productService.importData(rows, accountDto.getMerchantId(), accountDto.getAccountBookId());
            return JsonResult.successful();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new ServiceException(e.getMessage());
        }
    }
}
