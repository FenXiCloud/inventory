package com.flyemu.share.controller.setting;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.Admin;
import com.flyemu.share.service.setting.AdminService;
import com.flyemu.share.service.setting.DDLoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@SaCheckLogin
public class AdminController {

    private final AdminService adminService;
    private final DDLoginService ddLoginService;

    @GetMapping
    public JsonResult list(AdminService.Query query, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(adminService.query(accountDto.getMerchantId(), query));
    }

    @PostMapping
    public JsonResult save(@RequestBody @Valid Admin admin, @SaAccountVal AccountDto accountDto) {
        Assert.isNull(admin.getId(), "新增管理员Id必须为空~");
        TenantScope.bindMerchant(accountDto, admin::setMerchantId);
        adminService.save(admin);
        return JsonResult.successful();
    }

    @PutMapping
    public JsonResult update(@RequestBody @Valid Admin admin, @SaAccountVal AccountDto accountDto) {
        Assert.notNull(admin.getId(), "更新管理员Id不允许为空~");
        TenantScope.bindMerchant(accountDto, admin::setMerchantId);
        adminService.save(admin);
        return JsonResult.successful();
    }

    @DeleteMapping("/{adminId}")
    public JsonResult delete(@PathVariable Long adminId, @SaAdminId Integer saAdminId, @SaAccountVal AccountDto accountDto) {
        Assert.isFalse(saAdminId.equals(adminId), "不允许删除自己~");
        adminService.delete(adminId, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping("/reset/password/{adminId}")
    public JsonResult resetPassword(@PathVariable Long adminId, @SaAccountVal AccountDto accountDto) {
        adminService.resetPassword(adminId, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @PutMapping("/update/password")
    public JsonResult updatePassword(String oldPassword, String newPassword, @SaAdminId Long adminId, @SaAccountVal AccountDto accountDto) {
        adminService.updatePassword(adminId, oldPassword, newPassword, accountDto.getMerchantId());
        return JsonResult.successful();
    }

    @GetMapping("/addUserByDingDing")
    public JsonResult addUserByDingDing() {
        return JsonResult.successful(ddLoginService.submitUserSync());
    }

    @GetMapping("/dingDingSyncProgress")
    public JsonResult dingDingSyncProgress() {
        return JsonResult.successful(ddLoginService.getSyncProgress());
    }
}
