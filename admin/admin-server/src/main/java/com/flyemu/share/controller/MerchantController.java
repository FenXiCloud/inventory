package com.flyemu.share.controller;

import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.Merchant;
import com.flyemu.share.service.MerchantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * @功能描述: 商户管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/merchant")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    @GetMapping
    public JsonResult list(Page page, MerchantService.Query query) {
        return JsonResult.successful(merchantService.query(page, query));
    }

    @GetMapping("/select")
    public JsonResult list() {
        return JsonResult.successful(merchantService.select());
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Merchant merchant) {
        Assert.isNull(merchant.getId(), "新增商户Id必须为空~");
        merchantService.save(merchant);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Merchant merchant) {
        Assert.notNull(merchant.getId(), "更新商户Id不允许为空~");
        merchantService.save(merchant);
        return JsonResult.successful();
    }

    @DeleteMapping("/{merchantId}")
    public JsonResult delete(@PathVariable Long merchantId) {
        merchantService.delete(merchantId);
        return JsonResult.successful();
    }

}
