package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.annotation.SaUserVal;
import com.flyemu.share.common.Constants;
import com.flyemu.share.entity.MerchantUser;
import com.flyemu.share.service.MerchantUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class AppController {

    private final MerchantUserService merchantUserService;

    @GetMapping("/")
    public JsonResult index() {
        return JsonResult.successful(LocalDateTime.now());
    }

    @GetMapping("/init")
    @SaCheckLogin
    public JsonResult init(@SaUserVal MerchantUser user) {
        return JsonResult.successful(user);
    }

    @PostMapping("/login")
    public JsonResult login(String username, String password) {
        MerchantUser merchantUser = merchantUserService.login(username, password);
        StpUtil.login("admin"+merchantUser.getId(), "pc");
        SaSession session = StpUtil.getSession();
        session.set(Constants.SESSION_ACCOUNT, merchantUser);
        return JsonResult.successful(merchantUser);
    }

    @GetMapping("/logout")
    @SaCheckLogin
    public JsonResult logout(@SaUserVal MerchantUser merchantUser) {
        StpUtil.logout("admin"+merchantUser.getId(), "pc");
        return JsonResult.successful();
    }
}
