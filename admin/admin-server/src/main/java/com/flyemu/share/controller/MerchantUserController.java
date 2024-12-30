package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.MerchantUser;
import com.flyemu.share.service.MerchantUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * @功能描述: 商户管理员管理
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@RestController
@RequestMapping("/merchant/user")
@RequiredArgsConstructor
@SaCheckLogin
public class MerchantUserController {

    private final MerchantUserService merchantUserService;

    @GetMapping
    public JsonResult list(Page page) {

        return JsonResult.successful(merchantUserService.query(page));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid MerchantUser merchantUser) {
        Assert.isNull(merchantUser.getId(), "新增管理员Id必须为空~");
        merchantUserService.save(merchantUser);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid MerchantUser merchantUser) {
        Assert.notNull(merchantUser.getId(), "更新管理员Id不允许为空~");
        merchantUserService.save(merchantUser);
        return JsonResult.successful();
    }


    @DeleteMapping("/{userId}")
    public JsonResult delete(@PathVariable Long userId, Long merchantId) {
        merchantUserService.delete(userId, merchantId);
        return JsonResult.successful();
    }

    @PutMapping("/update/password/{userId}")
    public JsonResult updatePassword(@PathVariable Long userId, String oldPassword, String newPassword) {
        merchantUserService.updatePassword(userId, oldPassword, newPassword);
        return JsonResult.successful();
    }

    @PutMapping("/reset/password/{userId}")
    public JsonResult resetPassword(@PathVariable Long userId, Long merchantId) {
        merchantUserService.resetPassword(userId, merchantId);
        return JsonResult.successful();
    }
}
