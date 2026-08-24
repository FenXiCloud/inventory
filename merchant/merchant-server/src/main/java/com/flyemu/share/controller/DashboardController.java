package com.flyemu.share.controller;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    public JsonResult overview(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(dashboardService.overview(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
