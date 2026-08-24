package com.flyemu.share.controller.fund;

import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.service.fund.AdvanceBalanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fund/report")
@RequiredArgsConstructor
public class AdvanceBalanceController {

    private final AdvanceBalanceService advanceBalanceService;

    @GetMapping("/advance-balance")
    public JsonResult advanceBalance(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(advanceBalanceService.advanceBalance(accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
