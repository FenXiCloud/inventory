package com.flyemu.share.controller.setting;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.TenantScope;
import com.flyemu.share.common.Constants;
import com.flyemu.share.controller.JsonResult;
import com.flyemu.share.controller.Page;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.setting.Checkout;
import com.flyemu.share.service.setting.CheckoutService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckoutController {

    private final CheckoutService checkoutService;

    /**
     * 分页查询
     */
    @GetMapping
    public JsonResult list(Page page, CheckoutService.Query query, @SaAccountVal AccountDto accountDto) {
        if (accountDto.getAccountBookId() != null) {
            query.setAccountBookId(accountDto.getAccountBookId());
        }
        return JsonResult.successful(checkoutService.query(page, query));
    }

    /**
     * 结账前检查
     */
    @GetMapping("/preCheck")
    public JsonResult preCheck(@RequestParam LocalDate checkDate, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(checkoutService.preCheck(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), checkDate));
    }

    /**
     * 新增结账
     */
    @PostMapping
    public JsonResult save(@RequestBody @Valid Checkout checkout, @SaAccountVal AccountDto accountDto) {
        TenantScope.bind(checkout, accountDto);
        checkout.setCheckId(accountDto.getAdminId());
        LocalDate checkDate = checkoutService.save(checkout).getCheckDate();
        SaSession session = StpUtil.getTokenSession();
        accountDto.setCheckDate(checkDate);
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        return JsonResult.successful(checkDate);
    }

    /**
     * 反结账
     */
    @PutMapping
    public JsonResult cancelCheckout(@SaAccountVal AccountDto accountDto) {
        LocalDate checkDate = checkoutService.cancelCheckout(
                accountDto.getAccountBookId(), accountDto.getMerchantId(), accountDto.getAdminId());
        SaSession session = StpUtil.getTokenSession();
        accountDto.setCheckDate(checkDate);
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        return JsonResult.successful(checkDate);
    }

    /**
     * 查询月结库存表
     */
    @GetMapping("/monthlySummary")
    public JsonResult monthlySummary(@RequestParam LocalDate period, @SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(checkoutService.queryMonthlySummary(
                accountDto.getMerchantId(), accountDto.getAccountBookId(), period));
    }

    /**
     * 查询结账历史
     */
    @GetMapping("/history")
    public JsonResult history(@SaAccountVal AccountDto accountDto) {
        return JsonResult.successful(checkoutService.queryCloseHistory(
                accountDto.getMerchantId(), accountDto.getAccountBookId()));
    }
}
