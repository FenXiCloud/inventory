package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SaCheckLogin
public class AdminController {

    private final AdminService adminService;

    @GetMapping
    public JsonResult list(Page page, AdminService.Query query, @RequestParam(required = false) Long merchantId) {
        query.setMerchantId(merchantId);
        return JsonResult.successful(adminService.query(page, query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Admin admin) {
        adminService.save(admin);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Admin admin) {
        adminService.save(admin);
        return JsonResult.successful();
    }

    @DeleteMapping("/{adminId}")
    public JsonResult delete(@PathVariable Long adminId, @RequestParam Long merchantId) {
        adminService.delete(adminId, merchantId);
        return JsonResult.successful();
    }

    @PutMapping("/reset/password/{adminId}")
    public JsonResult resetPassword(@PathVariable Long adminId, @RequestParam Long merchantId) {
        adminService.resetPassword(adminId, merchantId);
        return JsonResult.successful();
    }
}
