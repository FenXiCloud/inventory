package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.SupplierCategory;
import com.flyemu.share.service.SupplierCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 供货商分类管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/supplierCategory")
@RequiredArgsConstructor
@SaCheckLogin
public class SupplierCategoryController {

    private final SupplierCategoryService supplierCategoryService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, SupplierCategoryService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(supplierCategoryService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid SupplierCategory supplierCategory, @SaAccountVal AccountDto accountDto) {
        supplierCategory.setMerchantId(accountDto.getMerchantId());
        supplierCategory.setAccountBookId(accountDto.getAccountBookId());
        supplierCategoryService.save(supplierCategory);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid SupplierCategory supplierCategory, @SaAccountVal AccountDto accountDto) {
        supplierCategory.setMerchantId(accountDto.getMerchantId());
        supplierCategory.setAccountBookId(accountDto.getAccountBookId());
        supplierCategoryService.save(supplierCategory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{supplierCategoryId}")
    public JsonResult delete(@PathVariable Long supplierCategoryId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        supplierCategoryService.delete(supplierCategoryId, merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(supplierCategoryService.select(merchantId,accountBookId));
    }
}
