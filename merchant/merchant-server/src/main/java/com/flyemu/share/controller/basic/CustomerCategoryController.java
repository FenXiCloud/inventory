package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountBookId;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaMerchantId;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.CustomerCategory;
import com.flyemu.share.service.basic.CustomerCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 客户分类管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/customerCategory")
@RequiredArgsConstructor
@SaCheckLogin
public class CustomerCategoryController {

    private final CustomerCategoryService customerCategoryService;

    @GetMapping
    public JsonResult list(@SaAccountVal AccountDto accountDto, CustomerCategoryService.Query query) {
        query.setMerchantId(accountDto.getMerchantId());
        query.setAccountBookId(accountDto.getAccountBookId());
        return JsonResult.successful(customerCategoryService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerCategory customerCategory, @SaAccountVal AccountDto accountDto) {
        customerCategory.setMerchantId(accountDto.getMerchantId());
        customerCategory.setAccountBookId(accountDto.getAccountBookId());
        customerCategoryService.save(customerCategory);
        return JsonResult.successful();
    }


    @PutMapping
    public JsonResult update(@RequestBody @Valid CustomerCategory customerCategory, @SaAccountVal AccountDto accountDto) {
        customerCategory.setMerchantId(accountDto.getMerchantId());
        customerCategory.setAccountBookId(accountDto.getAccountBookId());
        customerCategoryService.save(customerCategory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerCategoryId}")
    public JsonResult delete(@PathVariable Long customerCategoryId, @SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        customerCategoryService.delete(customerCategoryId, merchantId,accountBookId);
        return JsonResult.successful();
    }

    @GetMapping("select")
    public JsonResult select(@SaMerchantId Long merchantId, @SaAccountBookId Long accountBookId) {
        return JsonResult.successful(customerCategoryService.select(merchantId,accountBookId));
    }
}
