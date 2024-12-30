package com.flyemu.share.controller;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.Assert;
import com.flyemu.share.annotation.SaAccountVal;
import com.flyemu.share.common.Constants;
import com.flyemu.share.dto.AccountDto;
import com.flyemu.share.entity.Checkout;
import com.flyemu.share.service.CheckoutService;
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
     *
     * @param page
     * @return
     */
    @GetMapping
    public JsonResult list(Page page, CheckoutService.Query query, @SaAccountVal AccountDto accountDto) {
        if (accountDto.getAccountBookId() != null) {
            query.setAccountBookId(accountDto.getAccountBookId());
        }
        return JsonResult.successful(checkoutService.query(page, query));
    }

    /**
     * 新增结账
     *
     * @param checkout
     * @return
     */
    @PostMapping
    public JsonResult save(@RequestBody @Valid Checkout checkout, @SaAccountVal AccountDto accountDto) {
        checkout.setMerchantId(accountDto.getMerchantId());
        checkout.setAccountBookId(accountDto.getAccountBookId());
        checkout.setCheckId(accountDto.getAdminId());
        LocalDate checkDate= checkoutService.save(checkout).getCheckDate();
        SaSession session = StpUtil.getTokenSession();
        accountDto.setCheckDate(checkDate);
        session.set(Constants.SESSION_ACCOUNT, accountDto);

        return JsonResult.successful(checkDate);
    }

    /**
     * 反结账
     *
     * @param accountDto
     * @return
     */
    @PutMapping
    public JsonResult cancelCheckout( @SaAccountVal AccountDto accountDto) {
        LocalDate checkDate = checkoutService.cancelCheckout(accountDto.getAccountBookId(),accountDto.getMerchantId());
        SaSession session = StpUtil.getTokenSession();
        accountDto.setCheckDate(checkDate);
        session.set(Constants.SESSION_ACCOUNT, accountDto);
        return JsonResult.successful(checkDate);
    }

}
