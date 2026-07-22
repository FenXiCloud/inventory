package com.flyemu.share.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.SaLoginModel;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.JakartaServletUtil;
import com.alibaba.fastjson.JSONObject;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.annotation.SaAdminId;
import com.flyemu.share.common.Constants;
import com.flyemu.share.common.PinYinUtil;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.dto.MenuDto;
import com.flyemu.share.entity.setting.SystemLog;
import com.flyemu.share.service.setting.AccountBookService;
import com.flyemu.share.service.setting.AdminService;
import com.flyemu.share.service.setting.DDLoginService;
import com.flyemu.share.service.setting.MerchantService;
import com.flyemu.share.service.setting.SystemLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class AppController {

    private final MerchantService merchantService;

    private final AdminService adminService;

    private final AccountBookService accountBookService;

    private final DDLoginService loginService;

    private final SystemLogService systemLogService;

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
    public JsonResult login(String username, String password, String device,
                            HttpServletRequest request, HttpServletResponse response) {
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
        response.addHeader("Authorization", StpUtil.getTokenValue());
        recordLoginLog(accountDto, request, "用户「" + username + "」登录系统",
                "device=" + StrUtil.blankToDefault(device, "pc"));
        return JsonResult.successful()
                .data("account", accountDto);
    }

    @GetMapping("/logout")
    @SaCheckLogin
    public JsonResult logout(@SaAccountVal AccountDto accountDto, @SaAdminId Long adminId, HttpServletRequest request) {
        try {
            if (accountDto != null) {
                String name = resolveAdminName(accountDto);
                systemLogService.record(
                        "系统登录",
                        SystemLog.OperationType.登录,
                        "用户「" + name + "」退出系统",
                        JakartaServletUtil.getClientIP(request),
                        null,
                        adminId,
                        adminId,
                        accountDto.getMerchantId(),
                        accountDto.getAccountBookId()
                );
            }
        } catch (Exception e) {
            log.warn("记录退出日志失败", e);
        }
        StpUtil.logout(adminId, "pc");
        return JsonResult.successful();
    }

    @GetMapping("/py")
    public JsonResult loadPY(@SaAdminId Long adminId, String name) {
        if (StrUtil.isNotEmpty(name)) {
            return JsonResult.successful(PinYinUtil.getFirstLettersUp(name));
        }
        return JsonResult.successful(null);
    }

    /**
     * 钉钉Code回调
     * @return 结果
     */
    @PostMapping("/dd/auth")
    public JsonResult getAccessToken(@RequestBody JSONObject jsonObject,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
        if (ObjectUtils.isEmpty(jsonObject.getString("corpId"))
                || ObjectUtils.isEmpty(jsonObject.getString("authCode"))) {
            throw new RuntimeException("登录失败");
        }
        String mobile = loginService.getLoginAuth(jsonObject.getString("authCode"),
                jsonObject.getString("corpId"));
        if (ObjectUtils.isEmpty(mobile)) {
            return JsonResult.failure("登录失败");
        }

        //获取业务系统的token
        AccountDto accountDto = adminService.ddLogin(mobile);
        StpUtil.login(accountDto.getAdminId(), "pc");
        SaSession session = StpUtil.getTokenSession();
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        response.addHeader("Authorization", StpUtil.getTokenValue());
        recordLoginLog(accountDto, request,
                "用户「" + resolveAdminName(accountDto) + "」钉钉登录系统",
                "corpId=" + jsonObject.getString("corpId"));
        return JsonResult.successful()
                .data("account", accountDto);
    }

    /**
     * financial财务系统登录
     * @return 结果
     */
    @PostMapping("/financial/login")
    public JsonResult getFinancialToken(String mobile,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        if (ObjectUtils.isEmpty(mobile)) {
            throw new RuntimeException("登录失败");
        }

        //获取业务系统的token
        AccountDto accountDto = adminService.ddLogin(mobile);
        StpUtil.login(accountDto.getAdminId(), "pc");
        SaSession session = StpUtil.getTokenSession();
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        response.addHeader("Authorization", StpUtil.getTokenValue());
        recordLoginLog(accountDto, request,
                "用户「" + resolveAdminName(accountDto) + "」财务系统登录",
                "mobile=" + mobile);
        return JsonResult.successful()
                .data("account", accountDto);
    }

    @GetMapping("/getToken")
    public JsonResult getToken() {
        return JsonResult.successful(loginService.getToken());
    }

    private void recordLoginLog(AccountDto accountDto, HttpServletRequest request, String description, String params) {
        try {
            systemLogService.record(
                    "系统登录",
                    SystemLog.OperationType.登录,
                    description,
                    JakartaServletUtil.getClientIP(request),
                    params,
                    accountDto.getAdminId(),
                    accountDto.getAdminId(),
                    accountDto.getMerchantId(),
                    accountDto.getAccountBookId()
            );
        } catch (Exception e) {
            log.warn("记录登录日志失败", e);
        }
    }

    private String resolveAdminName(AccountDto accountDto) {
        if (accountDto == null || accountDto.getAdmin() == null) {
            return "-";
        }
        if (StrUtil.isNotBlank(accountDto.getAdmin().getName())) {
            return accountDto.getAdmin().getName();
        }
        return StrUtil.blankToDefault(accountDto.getAdmin().getUsername(), String.valueOf(accountDto.getAdminId()));
    }
}
