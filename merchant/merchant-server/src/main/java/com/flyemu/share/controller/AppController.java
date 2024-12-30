package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.Constants;
import com.flyemu.share.common.PinYinUtil;
import com.flyemu.share.dto.*;
import com.flyemu.share.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @功能描述: 管理员
 * @创建时间: 2023年08月08日
 * @公司官网: www.fenxi365.com
 * @公司信息: 纷析云（杭州）科技有限公司
 * @公司介绍: 专注于财务相关软件开发, 企业会计自动化解决方案
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class AppController {

    private final MerchantService merchantService;

    private final AdminService adminService;

    private final AccountBookService accountBookService;

    @GetMapping("/")
    public JsonResult index() {
        return JsonResult.successful(LocalDateTime.now());
    }

    @GetMapping("/init")
    public JsonResult init(@SaAccountVal AccountDto accountDto) {
        if (accountDto != null) {
            //获取菜单数据
            List<MenuDto> menus = adminService.loadMenu(accountDto.getMerchantId(), accountDto.getRole());

            List<Dict> accountBooks = accountBookService.loadAccountBooks(accountDto.getMerchantId());

            return JsonResult.successful()
                    .data("account", accountDto)
                    .data("menus", menus)
                    .data("accountBooks", accountBooks);
        }
        return JsonResult.failure();
    }


    @GetMapping("/home/view")
    @SaCheckLogin
    public JsonResult homeView(@SaAccountVal AccountDto accountDto) {

        return JsonResult.successful();
    }

    @PostMapping("/login")
    public JsonResult login(String username, String password, String device, HttpServletResponse response) {
        AccountDto accountDto = adminService.login(username, password);
        if (StrUtil.isNotEmpty(device)) {
            StpUtil.login(accountDto.getAdminId(), SaLoginModel.create()
                    .setDevice(device)
                    .setIsLastingCookie(true)
                    .setTimeout(-1)
            );
        } else {
            StpUtil.login(accountDto.getAdminId(), "pc");
        }
        SaSession session = StpUtil.getTokenSession();
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        response.addHeader("Authorization",  StpUtil.getTokenValue());
          return JsonResult.successful()
                .data("account", accountDto);
    }

    @GetMapping("/logout")
    @SaCheckLogin
    public JsonResult logout(@SaAdminId Long adminId) {
        StpUtil.logout(adminId, "pc");
        return JsonResult.successful();
    }

    @GetMapping("/py")
    public JsonResult loadPY(@SaAdminId Long adminId,String name) {
        if (StrUtil.isNotEmpty(name)){
            return JsonResult.successful( PinYinUtil.getFirstLettersUp(name));
        }
        return JsonResult.successful(null);
    }

}
