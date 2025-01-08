package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.Supplier;
import com.flyemu.share.service.basic.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 供货商管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/supplier")
@RequiredArgsConstructor
@SaCheckLogin
public class SupplierController {

    private final SupplierService supplierService;


    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, Page page, SupplierService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(supplierService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Supplier supplier, @SaAccountVal AccountDto accountDto) {
        supplier.setMerchantId(accountDto.getMerchantId());
        supplier.setAccountBookId(accountDto.getAccountBookId());
        supplierService.save(supplier);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Supplier supplier, @SaAccountVal AccountDto accountDto) {
        supplier.setMerchantId(accountDto.getMerchantId());
        supplier.setAccountBookId(accountDto.getAccountBookId());
        supplierService.save(supplier);
        return JsonResult.successful();
    }

    @DeleteMapping("/{supplierId}")
    public JsonResult delete(@PathVariable Long supplierId, @SaMerchantId Long merchantId) {
        supplierService.delete(supplierId, merchantId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId,@SaAccountBookId Long accountBookId) {
        return JsonResult.successful(supplierService.select(merchantId,accountBookId));
    }


    @GetMapping("/product/select/{supplierId}")
    public JsonResult select(@PathVariable Long supplierId, @SaAccountBookId Long accountBookId, @SaMerchantId Long merchantId) {
        return JsonResult.successful(supplierService.selectProducts(supplierId, merchantId, accountBookId));
    }
}
