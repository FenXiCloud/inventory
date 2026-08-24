package com.flyemu.share.controller.fund;

import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.service.fund.FundReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/fund/report")
@RequiredArgsConstructor
public class FundReportController {

    private final FundReportService fundReportService;

    @GetMapping("/profit")
    public JsonResult profit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(fundReportService.profit(accountDto.getMerchantId(), accountDto.getAccountBookId(), start, end));
    }

    @GetMapping("/profit-monthly")
    public JsonResult profitMonthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(fundReportService.profitMonthly(accountDto.getMerchantId(), accountDto.getAccountBookId(), start, end));
    }
}
