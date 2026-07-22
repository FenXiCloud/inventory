package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.entity.setting.MerchantUser;
import com.flyemu.share.service.MerchantUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public JsonResult delete(@PathVariable Long userId, @RequestParam Long merchantId) {
        merchantUserService.delete(userId, merchantId);
        return JsonResult.successful();
    }

    @PutMapping("/update/password/{userId}")
    public JsonResult updatePassword(@PathVariable Long userId, String oldPassword, String newPassword) {
        merchantUserService.updatePassword(userId, oldPassword, newPassword);
        return JsonResult.successful();
    }

    @PutMapping("/reset/password/{userId}")
    public JsonResult resetPassword(@PathVariable Long userId, @RequestParam Long merchantId) {
        merchantUserService.resetPassword(userId, merchantId);
        return JsonResult.successful();
    }
}
