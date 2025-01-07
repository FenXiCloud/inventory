package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.form.ProductForm;
import com.flyemu.share.service.basic.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 商品列表
 * @创建时间: 2024年05月05日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/product")
@RequiredArgsConstructor
@SaCheckLogin
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public JsonResult list(Page page, ProductService.Query query, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        query.setMerchantId(merchantId);
        query.setAccountBookId(accountBookId);
        return JsonResult.successful(productService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody ProductForm productForm, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        productService.save(productForm, merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("/get/{productId}")
    public JsonResult loadProduct(@PathVariable Long productId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(productService.loadById(productId, merchantId));
    }

    @DeleteMapping("/{productId}")
    public JsonResult delete(@PathVariable Long productId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        productService.delete(productId, merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(productService.select(merchantId,accountBookId));
    }

//    @GetMapping("loadTo/order")
//    public JsonResult loadToOrder(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
//        return JsonResult.successful(productService.loadToOrder(merchantId,accountBookId));
//    }

    @GetMapping("/customerLevel/price/{productId}")
    public JsonResult customerLevelPrice(@PathVariable Long productId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(productService.customerLevelPrice(productId, merchantId, accountBookId));
    }

//    @GetMapping("/goods/price/{customersId}")
//    public JsonResult goodsPrice(@PathVariable Long customersId, @SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
//        return JsonResult.successful(productService.goodsPriceList(customersId, merchantId, accountBookId,null));
//    }

}
