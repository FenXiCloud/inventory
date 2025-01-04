package com.flyemu.share.controller;

import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.entity.basic.ProductCategory;
import com.flyemu.share.service.ProductCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 商品分类
 * @创建时间: 2024年05月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@Slf4j
@RequestMapping("/productCategory")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    public JsonResult list(@SaMerchantId Long merchantId, ProductCategoryService.Query query, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(productCategoryService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid ProductCategory productCategory, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        productCategory.setMerchantId(merchantId);
        productCategory.setAccountBookId(accountBookId);
        productCategoryService.save(productCategory);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid ProductCategory productCategory) {
        productCategoryService.save(productCategory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{productCategoryId}")
    public JsonResult delete(@PathVariable Long productCategoryId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        productCategoryService.delete(merchantId, productCategoryId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(productCategoryService.select(merchantId,accountBookId));
    }

}
