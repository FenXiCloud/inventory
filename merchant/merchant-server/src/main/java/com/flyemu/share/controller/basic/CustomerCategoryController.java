package com.flyemu.share.controller.basic;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.basic.CustomerCategory;
import com.flyemu.share.service.basic.CustomerCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customerCategory")
@RequiredArgsConstructor
@SaCheckLogin
public class CustomerCategoryController {

    private final CustomerCategoryService customerCategoryService;

    @GetMapping
    public JsonResult list(CustomerCategoryService.Query query, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(query, accountDto);
        return JsonResult.successful(customerCategoryService.query(query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid CustomerCategory customerCategory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerCategory, accountDto);
        customerCategoryService.save(customerCategory);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid CustomerCategory customerCategory, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(customerCategory, accountDto);
        customerCategoryService.save(customerCategory);
        return JsonResult.successful();
    }

    @DeleteMapping("/{customerCategoryId}")
    public JsonResult delete(@PathVariable Long customerCategoryId, @SaAccountVal AccountDto accountDto) {
        customerCategoryService.delete(customerCategoryId, accountDto.getMerchantId(), accountDto.getAccountBookId());
        return JsonResult.successful();
    }

    @GetMapping("/select")
    public JsonResult select(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(customerCategoryService.select(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
